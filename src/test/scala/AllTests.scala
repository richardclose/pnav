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
      val str = TheNav.render(sampleMenu, TheEntryState, TextRenderer)
      assert(str.isDefined)
    }

    test("check xml menu") {
      TheNav.render(sampleMenu, TheEntryState, XmlRenderer).foreach { xml: Element =>
        val xpath = XPathFactory.newInstance().newXPath()
        xpath.evaluate("""//entry[@enabled='false']""", xml, XPathConstants.NODESET) match {
          case ns: NodeList =>
            assert(ns.getLength == 2)

          case _ =>
            assert(false)
        }
      }
    }
  }
}

object Fixtures {

  object TheNav extends PNav[Int, String]

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
    def isEnabled(context: String): Boolean = !Seq("blah", "ban").contains(context)
    def isVisible(context: String): Boolean = context != "can"
    def isCurrent(context: String): Boolean = context == "app"
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
        elt.setAttribute("enabled", enabled.toString)
        Some(elt)
      } else {
        None
      }
    }
  }

  def xmlToStr(elt: dom.Element): String = {
    val xf = TransformerFactory.newInstance().newTransformer()
    xf.setOutputProperty(OutputKeys.METHOD, "xml")
    xf.setOutputProperty(OutputKeys.INDENT, "yes")
    val out = new StringWriter()
    xf.transform(new DOMSource(elt), new StreamResult(out))
    out.toString
  }


}
