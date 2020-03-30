import java.io.Closeable

import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.stream.Materializer
import com.typesafe.config.Config
import javax.sql.DataSource
import controllers.HomeController
import controllers.UsersController
import io.getquill._
import play.api.ApplicationLoader.Context
import play.api._
import play.api.db.evolutions.Evolutions
import play.api.db.{DBComponents, HikariCPComponents}
import play.api.inject.{Injector, NewInstanceInjector, SimpleInjector}
import play.api.routing.Router
import play.api.routing.sird._
import play.api.libs.concurrent._
import models.Users
import play.api.i18n.I18nComponents
import play.api.mvc.{BodyParser, BodyParsers, DefaultControllerComponents, DefaultMessagesActionBuilderImpl, DefaultMessagesControllerComponents, EssentialFilter}
import play.filters.HttpFiltersComponents
import play.filters.components.GzipFilterComponents
import play.filters.gzip.GzipFilterConfig
import play.api.i18n._
import play.filters.csrf.CSRFComponents

import scala.concurrent.ExecutionContext

class AppLoader extends ApplicationLoader {
  override def load(context: Context): Application = new BuiltInComponentsFromContext(context)
    with DBComponents with HikariCPComponents with GzipFilterComponents with HttpFiltersComponents with I18nComponents
  with CSRFComponents {

    lazy val db = new H2JdbcContext(SnakeCase, dbApi.database("default").dataSource.asInstanceOf[DataSource with Closeable])

    lazy val users = new Users(db)
    lazy val usersController = new UsersController(users)(DefaultControllerComponents(
      defaultActionBuilder,
      playBodyParsers,
      messagesApi,
      langs,
      fileMimeTypes,
      executionContext
    )
    )
    lazy val homeController = new HomeController(controllerComponents)

    val router = Router.from {
      case GET(p"/")                     => homeController.index
      case GET(p"/users/${long(id)}")    => usersController.get(id)
      case GET(p"/users")                => usersController.allActive
      case POST(p"/users")               => usersController.create
      case DELETE(p"/users/${long(id)}") => usersController.delete(id)
      case PUT(p"/users/${long(id)}")    => usersController.update(id)
    }

    override lazy val injector: Injector =
      new SimpleInjector(NewInstanceInjector) + users + router + cookieSigner + csrfTokenSigner + httpConfiguration + tempFileCreator// + global

    Evolutions.applyEvolutions(dbApi.database("default"))

    override lazy val httpFilters: Seq[EssentialFilter] = gzipFilter() +: super.httpFilters


    override def config(): Config = configuration.underlying//super[HttpFiltersComponents].config()
    override implicit lazy val materializer: Materializer = super[GzipFilterComponents].materializer
    override implicit lazy val executionContext: ExecutionContext = super[GzipFilterComponents].executionContext

    override def configuration: Configuration = super[BuiltInComponentsFromContext].configuration
  }.application
}