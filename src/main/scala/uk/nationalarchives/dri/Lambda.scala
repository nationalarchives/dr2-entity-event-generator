package uk.nationalarchives.dri

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.typesafe.config.{ConfigFactory, Config => TypeSafeConfig}
import io.circe.Encoder
import software.amazon.awssdk.services.dynamodb.model._
import sttp.capabilities.fs2.Fs2Streams
import uk.gov.nationalarchives.DADynamoDBClient.DynamoDbRequest
import uk.gov.nationalarchives.dp.client.Entities.Entity
import uk.gov.nationalarchives.dp.client.EntityClient
import uk.gov.nationalarchives.dp.client.fs2.Fs2Client
import uk.gov.nationalarchives.{DADynamoDBClient, DASNSClient}

import java.time.OffsetDateTime
import java.util.{Date, TimeZone}

class Lambda extends RequestHandler[ScheduledEvent, Unit] {
  private val configFactory: TypeSafeConfig = ConfigFactory.load
  private val apiUrl: String = configFactory.getString("api.url")
  private val tableName: String = configFactory.getString("api.lastPolledTableName")
  private val secretName: String = configFactory.getString("api.secretName")
  private val snsArn: String = configFactory.getString("api.snsArn")
  private val maxEntitiesPerPage: Int = 1000
  private val dateItemPrimaryKeyAndValue =
    Map("id" -> AttributeValue.builder().s("LastPolled").build())
  private val datetimeField = "datetime"

  def entitiesClientIO: IO[EntityClient[IO, Fs2Streams[IO]]] = Fs2Client.entityClient(apiUrl)

  def dADynamoDBClient: DADynamoDBClient[IO] = DADynamoDBClient[IO]()

  def dASnsDBClient: DASNSClient[IO] = DASNSClient[IO]()

  implicit val enc: Encoder[Entity] =
    Encoder.forProduct5("entityType", "ref", "title", "deleted", "path")(entity =>
      (entity.entityType, entity.ref, entity.title, entity.deleted, entity.path)
    )

  override def handleRequest(event: ScheduledEvent, context: Context): Unit = {
    val offset = if (TimeZone.getTimeZone("Europe/London").inDaylightTime(new Date())) "+0100" else "+0000"
    val datetimeOfEventString: String = event.getTime.toString().replace("Z", offset)
    val eventTriggeredDatetime: OffsetDateTime = OffsetDateTime.parse(datetimeOfEventString)

    val entities = for {
      entitiesClient <- entitiesClientIO
      numOfEntitiesUpdated <- publishUpdatedEntitiesAndUpdateDateTime(entitiesClient, 0, eventTriggeredDatetime)
    } yield numOfEntitiesUpdated

    val _ = entities.unsafeRunSync()
  }

  private def publishUpdatedEntitiesAndUpdateDateTime(
      entityClient: EntityClient[IO, Fs2Streams[IO]],
      startFrom: Int,
      eventTriggeredDatetime: OffsetDateTime
  ): IO[Int] =
    for {
      numOfRecentlyUpdatedEntities <- getEntitiesUpdatedAndUpdateDB(entityClient, startFrom, eventTriggeredDatetime)
      _ <-
        if (numOfRecentlyUpdatedEntities > 0)
          publishUpdatedEntitiesAndUpdateDateTime(entityClient, startFrom + maxEntitiesPerPage, eventTriggeredDatetime)
        else IO(numOfRecentlyUpdatedEntities)
    } yield numOfRecentlyUpdatedEntities

  private def getEntitiesUpdatedAndUpdateDB(
      entitiesClient: EntityClient[IO, Fs2Streams[IO]],
      startFrom: Int,
      eventTriggeredDatetime: OffsetDateTime
  ): IO[Int] =
    for {
      updatedSinceAttributes <- dADynamoDBClient.getAttributeValues(
        DynamoDbRequest(tableName, dateItemPrimaryKeyAndValue, Map(datetimeField -> None))
      )
      updatedSinceAttributeValue = updatedSinceAttributes(datetimeField)
      updatedSinceAsDate = OffsetDateTime.parse(updatedSinceAttributeValue.s()).toZonedDateTime
      recentlyUpdatedEntities <- entitiesClient.entitiesUpdatedSince(updatedSinceAsDate, secretName, startFrom)
      _ <- IO.println(s"There were ${recentlyUpdatedEntities.length} entities updated since $updatedSinceAsDate")

      entityLastEventActionDate <-
        if (recentlyUpdatedEntities.nonEmpty) {
          val lastUpdatedEntity: Entity = recentlyUpdatedEntities.last
          entitiesClient.entityEventActions(lastUpdatedEntity, secretName).map { entityEventActions =>
            Some(entityEventActions.head.dateOfEvent.toOffsetDateTime)
          }
        } else IO(None)

      lastEventActionBeforeEventTriggered = entityLastEventActionDate.map(_.isBefore(eventTriggeredDatetime))

      _ <-
        if (lastEventActionBeforeEventTriggered.getOrElse(false)) {
          for {
            _ <- dASnsDBClient.publish[Entity](snsArn)(recentlyUpdatedEntities.toList.take(10))
            updateDateAttributeValue = AttributeValue.builder().s(entityLastEventActionDate.get.toString).build()
            updateDateRequest = DynamoDbRequest(
              tableName,
              dateItemPrimaryKeyAndValue,
              Map(datetimeField -> Some(updateDateAttributeValue))
            )
            statusCode <- dADynamoDBClient.updateAttributeValues(updateDateRequest)
          } yield statusCode
        } else IO(0)
    } yield recentlyUpdatedEntities.length
}
