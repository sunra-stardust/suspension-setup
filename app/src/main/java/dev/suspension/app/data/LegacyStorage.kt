package dev.suspension.app.data

import kotlin.math.abs

/**
 * The pre-phase-3 storage (two DataStores): `app_settings` (weight, temperature, selected and
 * custom fork/shock) and `scenario_values` (absolute overrides, key `"<rowId>:<scenarioIndex>"`).
 *
 * - [import] turns it into the first bike of the garage. Every value the old app showed —
 *   including the former built-in scenario offsets — becomes a relative [Edit], so the rider sees
 *   exactly the same numbers after the update (owner decision 2026-09-30).
 * - [mirror] writes the garage's first bike back in the old format after every change, so a
 *   rolled-back release still shows the latest values (CLAUDE.md rule 3).
 *
 * Pure functions over key → value maps; the DataStore plumbing lives in [LegacyStores].
 */
object LegacyStorage {
    // Key names exactly as the pre-phase-3 releases wrote them — never rename.
    const val WEIGHT = "rider_weight_kg"
    const val TEMP = "ride_temp_c"
    const val FORK_ID = "selected_fork_id"
    const val SHOCK_ID = "selected_shock_id"
    const val CUSTOM_FORK_NAME = "custom_fork_name"
    const val CUSTOM_FORK_TRAVEL = "custom_fork_travel_mm"
    const val CUSTOM_FORK_LSC = "custom_fork_lsc_max"
    const val CUSTOM_FORK_HSC = "custom_fork_hsc_max"
    const val CUSTOM_FORK_SPLIT = "custom_fork_rebound_split"
    const val CUSTOM_FORK_REBOUND = "custom_fork_rebound_max"
    const val CUSTOM_FORK_HSR = "custom_fork_hsr_max"
    const val CUSTOM_FORK_PSI = "custom_fork_psi"
    const val CUSTOM_SHOCK_NAME = "custom_shock_name"
    const val CUSTOM_SHOCK_STROKE = "custom_shock_stroke_mm"
    const val CUSTOM_SHOCK_E2E = "custom_shock_eye_to_eye_mm"
    const val CUSTOM_SHOCK_LSC = "custom_shock_lsc_max"
    const val CUSTOM_SHOCK_HSC = "custom_shock_hsc_max"
    const val CUSTOM_SHOCK_SPLIT = "custom_shock_rebound_split"
    const val CUSTOM_SHOCK_REBOUND = "custom_shock_rebound_max"
    const val CUSTOM_SHOCK_HSR = "custom_shock_hsr_max"
    const val CUSTOM_SHOCK_RATE = "custom_shock_rate_lbs"
    const val CUSTOM_SHOCK_LEVER = "custom_shock_climb_lever"

    fun valueKey(rowId: String, scenario: Scenario) = "$rowId:${scenario.index}"

    /** Result of reading the old storage: the rider's conditions and the bike (id supplied by the caller). */
    data class Imported(val weightKg: Double, val tempC: Int, val bike: Bike)

    fun import(settings: Map<String, Any>, values: Map<String, Any>, profile: BikeProfile, bikeId: String): Imported {
        val weight = (settings[WEIGHT] as? Double)?.let { kotlin.math.round(it) } ?: DEFAULT_WEIGHT_KG
        val temp = (settings[TEMP] as? Int) ?: DEFAULT_TEMP_C
        val forkId = settings[FORK_ID] as? String ?: profile.stockForkId
        val shockId = settings[SHOCK_ID] as? String ?: profile.stockShockId
        val customFork = customFork(settings)
        val customShock = customShock(settings, profile)
        val hasCustomFork = settings.keys.any { it.startsWith("custom_fork_") }
        val hasCustomShock = settings.keys.any { it.startsWith("custom_shock_") }

        val bike = Bike(
            id = bikeId,
            name = null,
            profileId = profile.id,
            forkId = forkId,
            shockId = shockId,
            customFork = customFork.takeIf { hasCustomFork || forkId == CUSTOM_ID },
            customShock = customShock.takeIf { hasCustomShock || shockId == CUSTOM_ID },
            springLbs = null,
            edits = emptyMap(),
        )
        val rows = rows(bike, profile, weight, temp)
        val edits = Scenario.ordered.associate { scenario ->
            scenario.tag to rows.mapNotNull { row ->
                val stored = values[valueKey(row.id, scenario)]
                val edit = when (row) {
                    is RowSpec.Stepper -> RowValues.editFor(row, (stored as? Double) ?: row.defaults[scenario.index])
                    is RowSpec.Toggle -> RowValues.editFor(row, (stored as? String) ?: row.defaults[scenario.index])
                }
                edit?.let { row.id to it }
            }.toMap()
        }.filterValues { it.isNotEmpty() }
        return Imported(weight, temp, bike.copy(edits = edits))
    }

    /** Old-format settings for [bike]: what the old app needs to show the same bike. */
    fun mirrorSettings(bike: Bike, weightKg: Double, tempC: Int, profile: BikeProfile): Map<String, Any> = buildMap {
        put(WEIGHT, weightKg)
        put(TEMP, tempC)
        // The old app only knows catalog ids and "custom"; anything else falls back to stock there anyway.
        put(FORK_ID, bike.forkId)
        put(SHOCK_ID, bike.shockId)
        bike.customFork?.let { f ->
            put(CUSTOM_FORK_NAME, f.displayName)
            put(CUSTOM_FORK_TRAVEL, f.travelMm)
            put(CUSTOM_FORK_LSC, f.lscMax)
            put(CUSTOM_FORK_HSC, f.hscMax ?: -1)
            put(CUSTOM_FORK_SPLIT, f.reboundMode == ReboundMode.SPLIT)
            put(CUSTOM_FORK_REBOUND, f.reboundMax)
            put(CUSTOM_FORK_HSR, f.hsrMax ?: -1)
            put(CUSTOM_FORK_PSI, f.baselinePsi)
        }
        bike.customShock?.let { s ->
            put(CUSTOM_SHOCK_NAME, s.displayName)
            put(CUSTOM_SHOCK_STROKE, kotlin.math.round(s.strokeMm).toInt())
            put(CUSTOM_SHOCK_E2E, s.eyeToEyeMm)
            put(CUSTOM_SHOCK_LSC, s.lscMax)
            put(CUSTOM_SHOCK_HSC, s.hscMax ?: -1)
            put(CUSTOM_SHOCK_SPLIT, s.reboundMode == ReboundMode.SPLIT)
            put(CUSTOM_SHOCK_REBOUND, s.reboundMax)
            put(CUSTOM_SHOCK_HSR, s.hsrMax ?: -1)
            put(CUSTOM_SHOCK_RATE, s.customSpringLbs ?: profile.stockSpringLbs)
            put(CUSTOM_SHOCK_LEVER, s.hasClimbLever)
        }
    }

    /**
     * Old-format values for [bike]: the absolute value every row shows in each built-in Vorlage
     * right now. Written for every row (not only edited ones) because the old app's defaults still
     * carry the former scenario offsets.
     */
    fun mirrorValues(bike: Bike, weightKg: Double, tempC: Int, profile: BikeProfile): Map<String, Any> {
        val rows = rows(bike, profile, weightKg, tempC)
        return buildMap {
            for (scenario in Scenario.ordered) {
                val edits = bike.editsFor(scenario.tag)
                for (row in rows) {
                    val value: Any = when (row) {
                        is RowSpec.Stepper -> RowValues.stepper(row, edits[row.id])
                        is RowSpec.Toggle -> RowValues.toggle(row, edits[row.id])
                    }
                    put(valueKey(row.id, scenario), value)
                }
            }
        }
    }

    /** Order-independent summary of the old storage, to notice changes made by a rolled-back release. */
    fun fingerprint(settings: Map<String, Any>, values: Map<String, Any>): String =
        (settings.entries.map { "s:${it.key}=${normalize(it.value)}" } + values.entries.map { "v:${it.key}=${normalize(it.value)}" })
            .sorted().joinToString("\n").hashCode().toUInt().toString(16)

    private fun normalize(value: Any): String = when (value) {
        is Double -> if (abs(value - kotlin.math.round(value)) < 1e-9) kotlin.math.round(value).toLong().toString() else value.toString()
        else -> value.toString()
    }

    private fun rows(bike: Bike, profile: BikeProfile, weightKg: Double, tempC: Int): List<RowSpec> {
        val fork = BikeParts.fork(bike, profile)
        val shock = BikeParts.shock(bike, profile)
        return listOfNotNull(
            ScenarioData.buildForkGroup(fork, weightKg, tempC),
            ScenarioData.buildShockGroup(shock, weightKg, tempC, profile),
            ScenarioData.buildTiresGroup(profile, tempC),
            ScenarioData.buildFrameGroup(profile),
        ).flatMap { it.rows }
    }

    // Defaults as the pre-phase-3 SettingsRepository used them for keys that were never written.
    private fun customFork(s: Map<String, Any>): ForkModel {
        return ForkModel(
            id = CUSTOM_ID,
            displayName = (s[CUSTOM_FORK_NAME] as? String)?.takeIf { it.isNotBlank() } ?: "",
            travelMm = s[CUSTOM_FORK_TRAVEL] as? Int ?: 170,
            lscMax = s[CUSTOM_FORK_LSC] as? Int ?: 16,
            hscMax = (s[CUSTOM_FORK_HSC] as? Int ?: 8).takeIf { it >= 0 },
            reboundMode = if (s[CUSTOM_FORK_SPLIT] as? Boolean ?: true) ReboundMode.SPLIT else ReboundMode.SINGLE,
            reboundMax = s[CUSTOM_FORK_REBOUND] as? Int ?: 16,
            hsrMax = (s[CUSTOM_FORK_HSR] as? Int ?: 8).takeIf { it >= 0 },
            pressureChart = null,
            baselinePsi = s[CUSTOM_FORK_PSI] as? Double ?: 100.0,
            maxPressurePsi = null,
            spacersStock = null,
            spacersMax = null,
        )
    }

    private fun customShock(s: Map<String, Any>, profile: BikeProfile) = ShockModel(
        id = CUSTOM_ID,
        displayName = (s[CUSTOM_SHOCK_NAME] as? String)?.takeIf { it.isNotBlank() } ?: "",
        strokeMm = (s[CUSTOM_SHOCK_STROKE] as? Int ?: 65).toDouble(),
        eyeToEyeMm = s[CUSTOM_SHOCK_E2E] as? Int ?: 205,
        lscMax = s[CUSTOM_SHOCK_LSC] as? Int ?: 16,
        hscMax = (s[CUSTOM_SHOCK_HSC] as? Int ?: 8).takeIf { it >= 0 },
        reboundMode = if (s[CUSTOM_SHOCK_SPLIT] as? Boolean ?: true) ReboundMode.SPLIT else ReboundMode.SINGLE,
        reboundMax = s[CUSTOM_SHOCK_REBOUND] as? Int ?: 16,
        hsrMax = (s[CUSTOM_SHOCK_HSR] as? Int ?: 8).takeIf { it >= 0 },
        hasClimbLever = s[CUSTOM_SHOCK_LEVER] as? Boolean ?: true,
        customSpringLbs = s[CUSTOM_SHOCK_RATE] as? Double ?: profile.stockSpringLbs,
        preloadHintResId = dev.suspension.app.R.string.hint_s_pre_generic,
        preloadRangeResId = dev.suspension.app.R.string.range_preload_generic,
    )
}

/** Resolves a bike's fork and shock: catalog entry, the rider's own model, or the profile's stock part. */
object BikeParts {
    /**
     * The spring rate the bike shows in Basis — what a new bike created from it starts with. An air
     * shock has no spring row; the profile's stock spring is the neutral fallback then.
     */
    fun installedSpringLbs(bike: Bike, weightKg: Double, tempC: Int): Double {
        val profile = profile(bike)
        val row = ScenarioData.buildShockGroup(shock(bike, profile), weightKg, tempC, profile).rows
            .filterIsInstance<RowSpec.Stepper>().firstOrNull { it.id == "s_rate" } ?: return profile.stockSpringLbs
        return RowValues.stepper(row, bike.editsFor(Scenario.BASIS.tag)[row.id])
    }

    /** The bike's frame profile, with the bike's own installed spring where it has one. */
    fun profile(bike: Bike): BikeProfile {
        val base = BikeProfiles.byId(bike.profileId)
        return bike.springLbs?.let { base.copy(stockSpringLbs = it) } ?: base
    }

    fun stockFork(profile: BikeProfile): ForkModel = ComponentCatalog.forkById(profile.stockForkId) ?: ComponentCatalog.forks.first()
    fun stockShock(profile: BikeProfile): ShockModel = ComponentCatalog.shockById(profile.stockShockId) ?: ComponentCatalog.shocks.first()

    /** The rider's own fork, or a starting point for creating one. Name may be blank (UI supplies a default). */
    fun customFork(bike: Bike, profile: BikeProfile): ForkModel =
        bike.customFork ?: stockFork(profile).copy(id = CUSTOM_ID, displayName = "", modelYears = emptyList(), provenance = emptyMap())

    fun customShock(bike: Bike, profile: BikeProfile): ShockModel {
        val stored = bike.customShock
            ?: return stockShock(profile).copy(id = CUSTOM_ID, displayName = "", maker = null, modelYears = emptyList(), provenance = emptyMap())
        // Bikes created before the catalog knew air shocks stored the stock shock as coil. While the
        // rider hasn't confirmed the prefilled part (needsCheck), the catalog's spring type wins.
        val stockSpring = bike.catalogBikeId?.let(ComponentCatalog::bikeById)?.stockShock?.spring
        return if (stored.needsCheck && stockSpring == SpringType.AIR && !stored.isAir) {
            stored.copy(spring = SpringType.AIR, customSpringLbs = null)
        } else {
            stored
        }
    }

    /** A stored id that's no longer in the catalog (e.g. a removed model) falls back to the stock part. */
    fun fork(bike: Bike, profile: BikeProfile): ForkModel =
        if (bike.forkId == CUSTOM_ID) customFork(bike, profile) else ComponentCatalog.forkById(bike.forkId) ?: stockFork(profile)

    fun shock(bike: Bike, profile: BikeProfile): ShockModel =
        if (bike.shockId == CUSTOM_ID) customShock(bike, profile) else ComponentCatalog.shockById(bike.shockId) ?: stockShock(profile)
}
