package dev.suspension.app.data

/**
 * A stock fork or shock as the bike maker lists it. [catalogId] points at a [ComponentCatalog]
 * entry when we have that part; otherwise the part becomes the rider's own ("custom") part,
 * prefilled with [name] and whatever dimensions the bike maker publishes. [spring]: air or coil
 * as the maker states it; null = not stated.
 */
data class StockPart(
    val catalogId: String?,
    val name: String,
    val travelMm: Int? = null,
    val eyeToEyeMm: Int? = null,
    val strokeMm: Double? = null,
    val spring: SpringType? = null,
)

/**
 * One bike as sold: maker, model, trim (build kit) and model year(s) — the level at which the
 * stock parts are fixed. Read from catalog.json → "bikes"; every field names its source like
 * the component entries.
 */
data class BikeModel(
    val id: String,
    val maker: String,
    val model: String,
    val trim: String,
    val modelYears: List<Int>,
    /** Frame extras implemented in code ([BikeProfiles]); null = generic frame. */
    val profileId: String?,
    val stockFork: StockPart,
    val stockShock: StockPart,
    /** Stock coil spring per frame size as the maker lists it; empty = not published. */
    val springLbsBySize: Map<String, Double>,
    val provenance: Map<String, Provenance> = emptyMap(),
    /** Drive motor as the maker lists it ("Shimano EP6, DU-EP600"); null = no motor stated. */
    val motor: String? = null,
) {
    /** "Mondraker Level RR 2026" */
    val displayName: String
        get() = listOf(maker, model, trim, modelYears.joinToString("/")).filter { it.isNotBlank() }.joinToString(" ")

    /** Sizes the rider can pick; only those the maker publishes values for. */
    val sizes: List<String> get() = springLbsBySize.keys.toList()
}

/** Turns a catalog bike into the parts a new garage bike starts with. */
object BikeSetup {

    /** The stock fork: the catalog entry, or a prefilled custom fork when we don't have that part. */
    fun fork(part: StockPart): Pair<String, ForkModel?> {
        part.catalogId?.let { id -> if (ComponentCatalog.forkById(id) != null) return id to null }
        val template = ComponentCatalog.forks.first()
        return CUSTOM_ID to template.copy(
            id = CUSTOM_ID,
            displayName = part.name,
            travelMm = part.travelMm ?: template.travelMm,
            pressureChart = null,
            maxPressurePsi = null,
            lsrChart = null,
            hsrChart = null,
            spacersStock = null,
            spacersMax = null,
            chartSource = null,
            lscStart = null,
            hscStart = null,
            modelYears = emptyList(),
            provenance = emptyMap(),
            needsCheck = true,
        )
    }

    /** The stock shock: the catalog entry, or a prefilled custom shock when we don't have that part. */
    fun shock(part: StockPart, springLbs: Double?): Pair<String, ShockModel?> {
        part.catalogId?.let { id -> if (ComponentCatalog.shockById(id) != null) return id to null }
        val template = ComponentCatalog.shocks.first()
        val air = part.spring == SpringType.AIR
        return CUSTOM_ID to template.copy(
            id = CUSTOM_ID,
            displayName = part.name,
            eyeToEyeMm = part.eyeToEyeMm ?: template.eyeToEyeMm,
            strokeMm = part.strokeMm ?: template.strokeMm,
            customSpringLbs = if (air) null else springLbs,
            preloadHintResId = dev.suspension.app.R.string.hint_s_pre_generic,
            preloadRangeResId = dev.suspension.app.R.string.range_preload_generic,
            maker = null,
            modelYears = emptyList(),
            provenance = emptyMap(),
            needsCheck = true,
            spring = part.spring ?: SpringType.COIL,
        )
    }
}
