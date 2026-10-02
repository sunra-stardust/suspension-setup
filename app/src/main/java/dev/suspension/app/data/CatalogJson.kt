package dev.suspension.app.data

import dev.suspension.app.R
import org.json.JSONArray
import org.json.JSONObject

/** A document a catalog value comes from (catalog.json → "sources"). */
data class CatalogSource(
    val id: String,
    val title: String,
    val url: String?,
    /** Model years the document covers; empty = the document names none. */
    val modelYears: List<Int>,
    /** ISO date the document was read. */
    val retrieved: String,
)

/** Where one field of a catalog entry comes from, plus an optional remark (table column, footnote …). */
data class Provenance(val source: CatalogSource, val note: String? = null)

data class Catalog(
    val sources: Map<String, CatalogSource>,
    val forks: List<ForkModel>,
    val shocks: List<ShockModel>,
    val bikes: List<BikeModel> = emptyList(),
)

/**
 * Reads `catalog/catalog.json` (a Java resource, so the app and plain JVM tests read the same
 * file). Strict on purpose: an unknown source id, a missing field or a chart with the wrong
 * row count fails loudly — in the unit tests, long before a release.
 */
object CatalogJson {
    const val RESOURCE = "catalog/catalog.json"
    private const val SCHEMA_VERSION = 1

    fun load(): Catalog {
        val stream = CatalogJson::class.java.classLoader?.getResourceAsStream(RESOURCE)
            ?: error("Missing resource $RESOURCE")
        return parse(stream.bufferedReader(Charsets.UTF_8).use { it.readText() })
    }

    fun parse(json: String): Catalog {
        val root = JSONObject(json)
        val schema = root.getInt("schemaVersion")
        require(schema == SCHEMA_VERSION) { "Unsupported catalog schemaVersion $schema" }

        val sourcesJson = root.getJSONObject("sources")
        val sources = sourcesJson.keys().asSequence().associateWith { id ->
            val o = sourcesJson.getJSONObject(id)
            CatalogSource(
                id = id,
                title = o.getString("title"),
                url = o.optStringOrNull("url"),
                modelYears = o.getJSONArray("modelYears").ints(),
                retrieved = o.getString("retrieved").also {
                    require(Regex("""\d{4}-\d{2}-\d{2}""").matches(it)) { "Source $id: retrieved must be YYYY-MM-DD, was $it" }
                },
            )
        }

        val forks = root.getJSONArray("forks").objects().map { parseFork(Entry(it, sources)) }
        val shocks = root.getJSONArray("shocks").objects().map { parseShock(Entry(it, sources)) }
        val ids = (forks.map { it.id } + shocks.map { it.id })
        require(ids.size == ids.toSet().size) { "Duplicate catalog ids: ${ids.groupBy { it }.filter { it.value.size > 1 }.keys}" }
        require(CUSTOM_ID !in ids) { "\"$CUSTOM_ID\" is reserved for the rider's own model" }

        val bikes = root.optJSONArray("bikes")?.objects().orEmpty().map { parseBike(Entry(it, sources)) }
        val bikeIds = bikes.map { it.id }
        require(bikeIds.size == bikeIds.toSet().size) { "Duplicate bike ids: ${bikeIds.groupBy { it }.filter { it.value.size > 1 }.keys}" }
        for (b in bikes) {
            b.stockFork.catalogId?.let { id -> require(forks.any { it.id == id }) { "${b.id}.stockFork: unknown fork \"$id\"" } }
            b.stockShock.catalogId?.let { id -> require(shocks.any { it.id == id }) { "${b.id}.stockShock: unknown shock \"$id\"" } }
            b.profileId?.let { id -> require(BikeProfiles.all.any { it.id == id }) { "${b.id}.profile: unknown bike profile \"$id\"" } }
        }
        return Catalog(sources, forks, shocks, bikes)
    }

    /** One catalog entry; each field is `{ "value": …, "source": "<id>", "note": "…" }`. */
    private class Entry(val json: JSONObject, val sources: Map<String, CatalogSource>) {
        val id: String = json.getString("id")
        val provenance = LinkedHashMap<String, Provenance>()

        private fun field(name: String): JSONObject {
            val o = json.optJSONObject(name) ?: error("$id: missing field \"$name\"")
            val sourceId = o.getString("source")
            val source = sources[sourceId] ?: error("$id.$name: unknown source \"$sourceId\"")
            provenance[name] = Provenance(source, o.optStringOrNull("note"))
            return o
        }

        fun int(name: String): Int = field(name).getInt("value")
        fun double(name: String): Double = field(name).getDouble("value")
        fun bool(name: String): Boolean = field(name).getBoolean("value")
        fun string(name: String): String = field(name).getString("value")
        fun intOrNull(name: String): Int? = field(name).let { if (it.isNull("value")) null else it.getInt("value") }
        fun obj(name: String): JSONObject = field(name).getJSONObject("value")

        fun chartOrNull(name: String): WeightChart? {
            if (!json.has(name)) return null
            val values = field(name).getJSONArray("value")
            require(values.length() == WeightBrackets.COUNT) { "$id.$name: needs ${WeightBrackets.COUNT} rows, has ${values.length()}" }
            return WeightChart((0 until values.length()).map { values.getDouble(it) })
        }

        fun reboundMode(): ReboundMode = when (val v = string("reboundMode")) {
            "split" -> ReboundMode.SPLIT
            "single" -> ReboundMode.SINGLE
            else -> error("$id.reboundMode: expected split|single, was $v")
        }

        val name: String get() = json.getString("name")
        val maker: String get() = json.getString("maker")
        val modelYears: List<Int> get() = json.getJSONArray("modelYears").ints()
    }

    private fun parseFork(e: Entry): ForkModel {
        val pressureChart = e.chartOrNull("pressureChart")
        return ForkModel(
            id = e.id,
            displayName = e.name,
            travelMm = e.int("travelMm"),
            lscMax = e.int("lscMax"),
            hscMax = e.intOrNull("hscMax"),
            reboundMode = e.reboundMode(),
            reboundMax = e.int("reboundMax"),
            hsrMax = e.intOrNull("hsrMax"),
            pressureChart = pressureChart,
            maxPressurePsi = if (e.json.has("maxPressurePsi")) e.double("maxPressurePsi") else null,
            lsrChart = e.chartOrNull("lsrChart"),
            hsrChart = e.chartOrNull("hsrChart"),
            spacersStock = if (e.json.has("spacersStock")) e.intOrNull("spacersStock") else null,
            spacersMax = if (e.json.has("spacersMax")) e.intOrNull("spacersMax") else null,
            chartSource = e.maker,
            lscStart = if (e.json.has("lscStart")) e.intOrNull("lscStart") else null,
            hscStart = if (e.json.has("hscStart")) e.intOrNull("hscStart") else null,
            modelYears = e.modelYears,
            provenance = e.provenance.toMap(),
        )
    }

    private fun parseShock(e: Entry): ShockModel {
        val preloadFox = when (val guide = e.string("preloadGuide")) {
            "fox" -> true
            "generic" -> false
            else -> error("${e.id}.preloadGuide: expected fox|generic, was $guide")
        }
        return ShockModel(
            id = e.id,
            displayName = e.name,
            strokeMm = e.double("strokeMm"),
            eyeToEyeMm = e.int("eyeToEyeMm"),
            lscMax = e.int("lscMax"),
            hscMax = e.intOrNull("hscMax"),
            reboundMode = e.reboundMode(),
            reboundMax = e.int("reboundMax"),
            hsrMax = e.intOrNull("hsrMax"),
            hasClimbLever = e.bool("hasClimbLever"),
            preloadHintResId = if (preloadFox) R.string.hint_s_pre_fox else R.string.hint_s_pre_generic,
            preloadRangeResId = if (preloadFox) R.string.range_preload_fox else R.string.range_preload_generic,
            maker = e.maker,
            modelYears = e.modelYears,
            provenance = e.provenance.toMap(),
        )
    }

    /**
     * A bike as sold. `stockFork`/`stockShock` are `{ "catalogId": "<fork/shock id>" | null, "name": …,
     * "travelMm"/"eyeToEyeMm"/"strokeMm", "spring": "air"|"coil" … }`; optional `motor`; `springLbsBySize` is `{ "<size>": lbs }` and optional.
     */
    private fun parseBike(e: Entry): BikeModel {
        fun part(name: String): StockPart {
            val o = e.obj(name)
            return StockPart(
                catalogId = o.optStringOrNull("catalogId"),
                name = o.getString("name"),
                travelMm = o.optIntOrNull("travelMm"),
                eyeToEyeMm = o.optIntOrNull("eyeToEyeMm"),
                strokeMm = o.optDoubleOrNull("strokeMm"),
                spring = when (val s = o.optStringOrNull("spring")) {
                    null -> null
                    "air" -> SpringType.AIR
                    "coil" -> SpringType.COIL
                    else -> error("${e.id}.$name.spring: expected air|coil, was $s")
                },
            )
        }
        val springs = if (e.json.has("springLbsBySize")) e.obj("springLbsBySize") else JSONObject()
        return BikeModel(
            id = e.id,
            maker = e.maker,
            model = e.json.getString("model"),
            trim = e.json.optString("trim", ""),
            modelYears = e.modelYears,
            profileId = e.json.optStringOrNull("profile"),
            stockFork = part("stockFork"),
            stockShock = part("stockShock"),
            springLbsBySize = springs.keys().asSequence().associateWith { springs.getDouble(it) },
            motor = if (e.json.has("motor")) e.string("motor") else null,
            provenance = e.provenance.toMap(),
        )
    }

    private fun JSONObject.optDoubleOrNull(name: String): Double? = if (!has(name) || isNull(name)) null else getDouble(name)
    private fun JSONObject.optStringOrNull(name: String): String? = if (!has(name) || isNull(name)) null else getString(name)
    private fun JSONObject.optIntOrNull(name: String): Int? = if (!has(name) || isNull(name)) null else getInt(name)
    private fun JSONArray.ints(): List<Int> = (0 until length()).map { getInt(it) }
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
}
