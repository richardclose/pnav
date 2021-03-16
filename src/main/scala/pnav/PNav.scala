
package pnav

/**
 * Framework for defining a hierarchical menu system
 * @tparam A Action, type associated with selected action (e.g. URL)
 * @tparam C Context, type carrying additional information (e.g. for permissioning, access control ...)
 */
trait PNav[A,C] {

	/**
	 * Protocol for reading state of a menu entry from the context.
	 */
	trait EntryState {
		def isEnabled(context: C): Boolean
		def isVisible(context: C): Boolean
		def isCurrent(context: C): Boolean
	}

	/**
	 * No-op EntryState
	 */
	object nilEntryState extends EntryState {
		def isEnabled(context: C): Boolean = true
		def isVisible(context: C): Boolean = true
		def isCurrent(context: C): Boolean = false
	}

	/**
	 * Methods for rendering the navigation structure as display type.
	 * @tparam R Rendered display type (e.g. string, XML tag, Scalatag)
	 */
	trait Renderer[R] {
		def renderMenu(menu: Menu, renderedChildren: Seq[R], level: Int, enabled: Boolean, visible: Boolean, current: Boolean): Option[R]
		def renderEntry(entry: Entry, level: Int, enabled: Boolean, visible: Boolean, current: Boolean): Option[R]
	}

	/**
	 * Menu containing list of entries
	 * @param name display name
	 * @param children entries
	 */
	class Menu(val name: String, val children: Seq[Node]) {
		def apply(nodes: Node*): Node =
			Left(new Menu(name, nodes))
	}

	/**
	 * Menu entry
	 * @param context contextual information
	 * @param name display name
	 * @param action data for invocation action
	 */
	class Entry(val context: C, val name: String, val action: A)

	type Node = Either[Menu, Entry]

	/** create the root menu */
	def root(nodes: Node*): Option[Menu] = {
		menu("root").apply(nodes: _*)
			.fold(m => Some(m), _ => None)
	}

	/** create a submenu */
	def menu(name: String): Menu = new Menu(name, Seq.empty)

	/** create a menu entry */
	def entry(context: C, name: String, action: A): Node = {
		Right(new Entry(context, name, action))
	}

	/**
	 * Render the menu with given renderer
	 */
	def render[R](root: Menu, es: EntryState, renderer: Renderer[R]): Option[R] = {
		val esa = new EntryStateAccumulator(es)
		renderMenu(root, es, esa, 0, renderer)
	}

	// Implementation
	/*
   * Accumulates entry state info so we can visit each entry once.
	 */
	private class EntryStateAccumulator(entryState: EntryState) {
		var entryCount = 0
		var enabledCount = 0
		var visibleCount = 0
		var currentCount = 0

		def process(context: C): (Boolean, Boolean, Boolean) = {
			entryCount += 1
			val ena = entryState.isEnabled(context)
			val vis = entryState.isVisible(context)
			val cur = entryState.isCurrent(context)
			enabledCount += (if (ena) 1 else 0)
			visibleCount += (if (vis) 1 else 0)
			currentCount += (if (cur) 1 else 0)
			(ena, vis, cur)
		}

		def add(other: EntryStateAccumulator): Unit = {
			entryCount += other.entryCount
			enabledCount += other.enabledCount
			visibleCount += other.visibleCount
			currentCount += other.currentCount
		}

		def anyEnabled: Boolean = enabledCount > 0
		def anyVisible: Boolean = visibleCount > 0
		def anyCurrent: Boolean = currentCount > 0
	}

	private def renderChildren[R](menu: Menu, es: EntryState, esa: EntryStateAccumulator, level: Int, renderer: Renderer[R]): Seq[R] =
		menu.children.collect {
		case Left(m) =>
			renderMenu(m, es, esa, level + 1, renderer)

		case Right(e) =>
			val (ena, vis, cur) = esa.process(e.context)
			renderer.renderEntry(e, level + 1, ena, vis, cur)
	}.flatten

	private def renderMenu[R](menu: Menu, es: EntryState, esa: EntryStateAccumulator, level: Int, renderer: Renderer[R]): Option[R] = {
		val esaLocal = new EntryStateAccumulator(es)
		val renderedChildren = renderChildren(menu, es, esaLocal, level, renderer)
		val ret = renderer.renderMenu(menu, renderedChildren, level, esaLocal.anyEnabled, esaLocal.anyVisible, esaLocal.anyCurrent)
		esa.add(esaLocal)
		ret
	}

}




