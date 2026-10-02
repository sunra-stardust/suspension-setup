package dev.suspension.app.data

/**
 * What the app knows for sure about the selected bike, as far as texts depend on it. Content that
 * names a maker, a component or a motor is only shown when the matching trait is present; otherwise
 * a neutral variant (or nothing) is shown. Unknown is never treated as present.
 */
enum class Trait {
    /** E-bike with any drive (motor, display, charging port). */
    EBIKE,
    /** E-bike with a Bosch drive (Bosch battery statements, Mini Remote, Flow app). Implies [EBIKE]. */
    EBIKE_BOSCH,
    /** SRAM mineral-oil brakes (Maven). */
    BRAKES_SRAM_MINERAL,
    /** SRAM AXS wireless shifting (Pod / Rocker Paddle, derailleur battery). */
    SHIFTING_SRAM_AXS,
    /** ONOFF Pija dropper post (air-sprung, valve under the top cap). */
    DROPPER_ONOFF_PIJA,
    /** Mondraker Level RR frame: frame-only spare parts, flip-chip geometry. */
    FRAME_MONDRAKER_LEVEL,
    /** The frame has a flip chip the app knows (options, effect). */
    FLIP_CHIP,
    FORK_FOX,
    SHOCK_FOX,
    SHOCK_FOX_DHX2,
    /** Coil shock the app knows as coil (catalog shock, or a spring the rider entered himself). */
    SHOCK_COIL,
    /** Air shock (maker states it, or the rider set it in his own shock). */
    SHOCK_AIR,
}

/** [present]: one of [anyOf] must be there; otherwise none of them may be. A list of conditions must all hold. */
data class Cond(val anyOf: Set<Trait>, val present: Boolean = true) {
    fun matches(traits: Set<Trait>): Boolean = anyOf.any { it in traits } == present
}

fun has(vararg traits: Trait) = Cond(traits.toSet(), present = true)
fun lacks(vararg traits: Trait) = Cond(traits.toSet(), present = false)

fun List<Cond>.matches(traits: Set<Trait>): Boolean = all { it.matches(traits) }

/** The selected bike's traits plus the names texts may need ("Für deine Gabel (RockShox ZEB) …"). */
data class BikeTraits(val traits: Set<Trait>, val forkName: String = "", val shockName: String = "") {
    fun has(trait: Trait): Boolean = trait in traits
    fun allows(conditions: List<Cond>): Boolean = conditions.matches(traits)

    /** Fox quotes (ridefox.com, Fox sag figures) only make sense with a Fox fork or shock. */
    val foxOnBike: Boolean get() = has(Trait.FORK_FOX) || has(Trait.SHOCK_FOX)

    companion object {
        /** A bike the app knows nothing about: only neutral content. */
        val NONE = BikeTraits(emptySet())

        /** [catalogBike]: the bike as sold, when the garage bike was picked from the catalog (motor). */
        fun of(profile: BikeProfile, fork: ForkModel, shock: ShockModel, catalogBike: BikeModel? = null): BikeTraits {
            val t = profile.equipment.toMutableSet()
            catalogBike?.motor?.let { motor ->
                t += Trait.EBIKE
                if (motor.trim().startsWith("Bosch", ignoreCase = true)) t += Trait.EBIKE_BOSCH
            }
            if (Trait.EBIKE_BOSCH in t) t += Trait.EBIKE
            if (profile.flipChip != null) t += Trait.FLIP_CHIP
            if (fork.chartSource.equals("Fox", ignoreCase = true) || startsWithFox(fork.displayName)) t += Trait.FORK_FOX
            if (shock.maker.equals("Fox", ignoreCase = true) || startsWithFox(shock.displayName)) t += Trait.SHOCK_FOX
            if (Trait.SHOCK_FOX in t && (shock.id.startsWith("fox_dhx2") || shock.displayName.contains("DHX2", ignoreCase = true))) {
                t += Trait.SHOCK_FOX_DHX2
            }
            // Catalog shocks are all coil; a custom shock is coil when the rider entered it himself.
            // A shock prefilled from a bike's spec sheet (needsCheck) may well be an air shock.
            when {
                shock.isAir -> t += Trait.SHOCK_AIR
                !shock.isCustom || !shock.needsCheck -> t += Trait.SHOCK_COIL
            }
            return BikeTraits(t, fork.displayName, shock.displayName)
        }

        private fun startsWithFox(name: String) = Regex("""^\s*fox\b""", RegexOption.IGNORE_CASE).containsMatchIn(name)
    }
}
