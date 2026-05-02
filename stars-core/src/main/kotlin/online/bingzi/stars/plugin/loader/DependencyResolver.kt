package online.bingzi.stars.plugin.loader

import online.bingzi.stars.plugin.api.PluginDescription
import online.bingzi.stars.plugin.api.PluginException
import java.util.PriorityQueue

/**
 * Topologically sorts a list of PluginDescriptions using Kahn's algorithm.
 *
 * Edge semantics (A → B means "A must be loaded before B", i.e. B depends on A):
 *  - depend:     A.depend ∋ B  →  edge B → A  (A must precede A's dependent)
 *                               i.e. for each dep in desc.depend: edge dep → desc.name
 *  - softdepend: same as depend but silently skipped when dep is not present
 *  - loadbefore: A.loadbefore ∋ B  →  edge A → B  (A explicitly wants to load before B)
 */
object DependencyResolver {

    /**
     * @param descs all plugins to be sorted
     * @return plugins in load order (dependencies first)
     * @throws PluginException.PluginDependencyException on missing hard dependency or cycle
     */
    fun sort(descs: List<PluginDescription>): List<PluginDescription> {
        if (descs.isEmpty()) return emptyList()

        val byName: Map<String, PluginDescription> = descs.associateBy { it.name }

        // edges[a] = set of names that depend on a (i.e. a must be loaded before each of them)
        val edges = HashMap<String, MutableSet<String>>().apply {
            descs.forEach { put(it.name, mutableSetOf()) }
        }

        // in-degree: number of prerequisites each plugin still needs loaded
        val inDeg = HashMap<String, Int>().apply {
            descs.forEach { put(it.name, 0) }
        }

        // Helper: add a directed edge from→to (to depends on from).
        // Returns true only if the edge is new, preventing duplicate in-degree increments
        // when the same pair appears in both depend and softdepend lists.
        fun addEdge(from: String, to: String) {
            if (edges[from]!!.add(to)) {
                inDeg[to] = inDeg[to]!! + 1
            }
        }

        for (desc in descs) {
            // Hard dependencies — missing dep is a fatal error
            for (dep in desc.depend) {
                if (!byName.containsKey(dep)) {
                    throw PluginException.PluginDependencyException(
                        "plugin ${desc.name} depends on missing $dep"
                    )
                }
                // dep → desc.name  (dep must come before desc)
                addEdge(from = dep, to = desc.name)
            }

            // Soft dependencies — silently skip absent entries
            for (dep in desc.softdepend) {
                if (byName.containsKey(dep)) {
                    addEdge(from = dep, to = desc.name)
                }
            }

            // loadbefore: desc must be loaded before each listed target
            for (target in desc.loadbefore) {
                if (byName.containsKey(target)) {
                    // desc → target  (desc must come before target)
                    addEdge(from = desc.name, to = target)
                }
            }
        }

        // Kahn's BFS — use PriorityQueue for deterministic name-ordered tie-breaking
        val queue = PriorityQueue<String>(compareBy { it })
        inDeg.forEach { (name, deg) -> if (deg == 0) queue.add(name) }

        val result = ArrayList<PluginDescription>(descs.size)

        while (queue.isNotEmpty()) {
            val name = queue.poll()
            result.add(byName[name]!!)
            for (dependent in edges[name]!!) {
                val newDeg = inDeg[dependent]!! - 1
                inDeg[dependent] = newDeg
                if (newDeg == 0) queue.add(dependent)
            }
        }

        if (result.size != descs.size) {
            val remaining = inDeg.filter { it.value > 0 }.keys.sorted()
            throw PluginException.PluginDependencyException(
                "dependency cycle detected: $remaining"
            )
        }

        return result
    }
}
