# PNav

## Introduction
A system for defining and rendering hierarchical menus, e.g. navbars. My original
use case is to define a menu structure for a website navigation bar, conditionally
modify the rendered output (e.g. according to a permissioning scheme), and render
the output for a choice of CSS libraries.

## Usage
The unit tests include a complete example of usage.

Instantiate the `PNav` trait with the type `C` that will supply additional 
contextual information for each menu entry, the type `A` that will 
define the action resulting from a menu selection, and the type `R` that
defines the context in which the menu is rendered (e.g. an HTTP request).

```scala
import pnav.PNav
import java.net.URL

object MyNav extends PNav[String, Int]
```

Implement the `EntryState` trait to if you need (e.g.) access control:
```scala
object MyEntryState extends MyNav.EntryState {
  // TODO: implement methods
}
```

Implement a suitable renderer with the type `D` of an element of the rendered output,
and the type `R` of a runtime context, e.g. an incoming HTTP request:

```scala
import org.w3c.dom

object MyXmlRenderer extends MyNav.Renderer[dom.Element, play.api.Request[_]] {
  // TODO: implement.
}
```

Define your menu structure:
```scala
import MyNav._
val menu = root (
  entry(context=1, name="Login", action="/app/login"),
  menu (
    entry(context=2, name="Search", action="/app/search")
  )
)
```

And render it:
```scala
implicit val req = getCurrentRequest()
val xmlElement = MyNav.render(menu, MyEntryStste, MyXmlRenderer)
```

## Further Development
I will add one or two Renderers (possibly configurable) as I develop them 
for CSS libraries that I'm using in my personal projects.

Since the currently selected submenu and item depend only on the routng which
can be statically known, this would be a good application of Scala 3 macros.