package dev.suspension.app.data

import kotlin.math.roundToInt

/** Whether a damper has one rebound circuit or a split low/high-speed pair. */
enum class ReboundMode { SPLIT, SINGLE }

/**
 * A fork model's tuning envelope. Catalog entries come from catalog/catalog.json, where every
 * number names its source; anything we couldn't verify is null and the UI falls back to plain
 * starting values instead of inventing data.
 *
 * Adding a model = adding one entry to catalog.json. No code changes needed.
 */
data class ForkModel(
    val id: String,
    val displayName: String,
    val travelMm: Int,
    val lscMax: Int,
    val hscMax: Int?,
    val reboundMode: ReboundMode,
    /** LSR max when [ReboundMode.SPLIT], the single rebound max otherwise. */
    val reboundMax: Int,
    val hsrMax: Int?,
    /** Manufacturer air-pressure chart by rider weight; null = no verified chart (custom). */
    val pressureChart: WeightChart?,
    /** Pressure used when there's no chart (custom models). */
    val baselinePsi: Double = 100.0,
    val maxPressurePsi: Double?,
    /** Manufacturer rebound starting clicks by rider weight; null = no verified chart. */
    val lsrChart: WeightChart? = null,
    val hsrChart: WeightChart? = null,
    val spacersStock: Int?,
    val spacersMax: Int?,
    /** Short name of the chart's publisher for hints ("Fox"). */
    val chartSource: String? = null,
    /** Manufacturer's recommended starting clicks from closed; null = none published. */
    val lscStart: Int? = null,
    val hscStart: Int? = null,
    /** Model years the source documents cover; empty for custom models or when no document names one. */
    val modelYears: List<Int> = emptyList(),
    /** Source per catalog field (e.g. "pressureChart"); empty for custom models. */
    val provenance: Map<String, Provenance> = emptyMap(),
    /** Custom part prefilled from a catalog bike's stock part: the click ranges are placeholders the rider should check. */
    val needsCheck: Boolean = false,
) {
    val isCustom: Boolean get() = id == CUSTOM_ID
}

/**
 * A coil shock's tuning envelope. There's deliberately no spring-rate-by-weight table here:
 * the right rate depends on the frame's leverage ratio (RockShox says so explicitly), so the
 * recommendation lives in [BikeProfile.springRule], calibrated per frame.
 */
data class ShockModel(
    val id: String,
    val displayName: String,
    val strokeMm: Int,
    val eyeToEyeMm: Int,
    val lscMax: Int,
    val hscMax: Int?,
    val reboundMode: ReboundMode,
    val reboundMax: Int,
    val hsrMax: Int?,
    val hasClimbLever: Boolean,
    /** Installed spring for custom shocks; catalog shocks use the bike's stock spring. */
    val customSpringLbs: Double? = null,
    /** Preload guidance shown as the preload row's hint. */
    val preloadHintResId: Int,
    /** Preload range shown on Basics → Einstellbereiche. */
    val preloadRangeResId: Int,
    val maker: String? = null,
    /** Model years the source documents cover; empty for custom models or when no document names one. */
    val modelYears: List<Int> = emptyList(),
    /** Source per catalog field (e.g. "hscMax"); empty for custom models. */
    val provenance: Map<String, Provenance> = emptyMap(),
    /** Custom part prefilled from a catalog bike's stock part: the click ranges are placeholders the rider should check. */
    val needsCheck: Boolean = false,
) {
    val isCustom: Boolean get() = id == CUSTOM_ID
}

const val CUSTOM_ID = "custom"

/**
 * Verified fork and coil-shock catalog, read from `catalog/catalog.json` (model year, source and
 * retrieval date per value — rules in `.claude/skills/catalog-data/SKILL.md`). Deliberately
 * small: a wrong number here is worse than a missing model, and the custom-model entry covers
 * everything else.
 */
object ComponentCatalog {
    private val catalog: Catalog by lazy { CatalogJson.load() }

    val sources: Map<String, CatalogSource> get() = catalog.sources
    val forks: List<ForkModel> get() = catalog.forks
    val shocks: List<ShockModel> get() = catalog.shocks
    val bikes: List<BikeModel> get() = catalog.bikes

    fun forkById(id: String): ForkModel? = forks.find { it.id == id }
    fun shockById(id: String): ShockModel? = shocks.find { it.id == id }
    fun bikeById(id: String): BikeModel? = bikes.find { it.id == id }

    /** Bike makers in the catalog, alphabetically. */
    val bikeMakers: List<String> get() = bikes.map { it.maker }.distinct().sortedBy { it.lowercase() }
}

/** Preserves a click value's relative position in the old range when switching to a model with a different max. */
fun remapClick(oldValue: Double, oldMax: Int, newMax: Int): Double {
    if (oldMax <= 0) return 0.0
    val fraction = (oldValue / oldMax).coerceIn(0.0, 1.0)
    return (fraction * newMax).roundToInt().toDouble()
}

/** Rounds a computed rate/pressure to the nearest step (e.g. 25 lbs, 1 psi). */
fun roundToStep(value: Double, step: Double): Double {
    if (step <= 0.0) return value
    return kotlin.math.round(value / step) * step
}
