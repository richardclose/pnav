import org.w3c.dom
import org.w3c.dom.{Document, Element, NodeList}
import pnav.PNav
import utest._

import java.io.StringWriter
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.{OutputKeys, TransformerFactory}
import javax.xml.xpath.{XPathConstants, XPathFactory}

object AllTests extends TestSuite {

  import Fixtures._

  val tests: Tests = Tests {
    test("generate text menu") {
      implicit val req: RequestT = Fixtures.RequestT("app")
      val str = TheNav.render(sampleMenu, TheEntryState, TextRenderer)
      assert(str.isDefined)
    }

    test("check xml menu") {
      implicit val req: RequestT = Fixtures.RequestT("app")
      TheNav.render(sampleMenu, TheEntryState, XmlRenderer).foreach { xml: Element =>
        val nl = queryNodes("//entry[@enabled='false']", xml)
        assert(nl.getLength == 2)
      }
    }

    test("check current entry") {
      implicit val req: RequestT = Fixtures.RequestT("app")
      TheNav.render(sampleMenu, TheEntryState, XmlRenderer).foreach { xml: Element =>
        val nl = queryNodes("//entry[@current='true']", xml)
        assert(nl.getLength == 1)
      }
    }
  }
}

object Fixtures {

  case class RequestT(path: String)

  object TheNav extends PNav[Int, String, RequestT]

  import TheNav._


  def sampleMenu: Menu = {
    root (
      entry("blah", "Blah!", 1),
      entry("foo", "Foo!", 2),
      menu("Submenu") (
        entry("app", "Apple!", 3),
        entry("ban", "Banana!", 4),
        menu("Subsub-menu") (
          entry("can", "Canteloupe", 5)
        )
      )
    ).get
  }

  object TheEntryState extends TheNav.EntryState {
    def isEnabled(context: String)(implicit req: RequestT): Boolean = !Seq("blah", "ban").contains(context)
    def isVisible(context: String)(implicit req: RequestT): Boolean = context != "can"
    def isCurrent(context: String)(implicit req: RequestT): Boolean = req.path == context
  }

  object TextRenderer extends TheNav.Renderer[String] {

    val width = 50

    def renderMenu(menu: TheNav.Menu, renderedChildren: Seq[String], level: Int, enabled: Boolean, visible: Boolean, current: Boolean): Option[String] = {
      Some("|".padTo(level * 2, ' ') +
        s"${menu.name} enabled=$enabled" +
        " {\n" + renderedChildren.mkString("") +
        "|".padTo(level * 2, ' ') + "}\n")
    }

    def renderEntry(entry: TheNav.Entry, level: Int, enabled: Boolean, visible: Boolean, current: Boolean): Option[String] = {
      if (visible)
        Some("|".padTo(level * 2, ' ') + s"[ ${entry.name} [${entry.action}:${entry.context}] ena=$enabled vis=$visible cur=$current/]\n")
      else
        None
    }
  }

  object XmlRenderer extends TheNav.Renderer[dom.Element] {

    val doc: Document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()

    def renderMenu(menu: TheNav.Menu, renderedChildren: Seq[Element], level: Int, enabled: Boolean, visible: Boolean, current: Boolean): Option[Element] = {
      if (visible) {
        val elt = doc.createElement("menu")
        elt.setAttribute("title", menu.name)
        elt.setAttribute("enabled", enabled.toString)
        renderedChildren.foreach(elt.appendChild)
        Some(elt)
      } else {
        None
      }
    }

    def renderEntry(entry: TheNav.Entry, level: Int, enabled: Boolean, visible: Boolean, current: Boolean): Option[Element] = {
      if (visible) {
        val elt = doc.createElement("entry")
        elt.setTextContent(entry.name)
        elt.setAttribute("context", s"${entry.context}:${entry.action}")
        elt.setAttribute("current", current.toString)
        elt.setAttribute("enabled", enabled.toString)
        Some(elt)
      } else {
        None
      }
    }
  }

  def queryNodes(expression: String, element: Element): NodeList = {
    val xpath = XPathFactory.newInstance().newXPath()
    xpath.evaluate(expression, element, XPathConstants.NODESET).asInstanceOf[NodeList]
  }

  def xmlToStr(elt: dom.Element): String = {
    val xf = TransformerFactory.newInstance().newTransformer()
    xf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    xf.setOutputProperty(OutputKeys.METHOD, "xml")
    xf.setOutputProperty(OutputKeys.INDENT, "yes")
    val out = new StringWriter()
    xf.transform(new DOMSource(elt), new StreamResult(out))
    out.toString
  }


}
