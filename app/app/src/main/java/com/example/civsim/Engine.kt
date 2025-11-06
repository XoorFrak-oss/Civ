package com.example.civsim

enum class Role { SCOUT, GATHER, REST, RESEARCH, EDUCATE }
enum class Season { WINTER, SPRING, SUMMER, AUTUMN }
enum class Resource { WATER, FRUITS, WOOD }

data class Person(
    val name: String,
    val sex: Char,
    val age: Double,
    val hp: Int = 100,
    val fatigue: Int = 0,
    val morale: Int = 72,
    val role: Role = Role.REST,
    val alive: Boolean = true,
    val gather: Int = 0,
    val observe: Int = 0,
    val scout: Int = 0,
)

data class Tech(val key: String, val name: String, val complexity: Double, val progress: Double = 0.0, val discovered: Boolean = false, val prereq: String? = null)
data class Tile(val area: Double = 100.0, val explored: Double = 10.0, val biome: String = "Forêt tempérée", val risk: Double = 0.08) {
    val exploredPct: Double get() = (explored / area).coerceIn(0.0, 1.0)
}

data class State(
    val people: List<Person>,
    val inv: Map<Resource, Double> = mapOf(Resource.WATER to 1.0, Resource.FRUITS to 1.0, Resource.WOOD to 0.0),
    val tile: Tile = Tile(),
    val year: Int = 0,
    val season: Season = Season.WINTER,
    val obs: Tech = Tech("OBS","Observation du feu",60.0),
    val fire: Tech = Tech("FIRE","Feu contrôlé",220.0, prereq = "OBS"),
    val hasHearth: Boolean = false,
    val log: List<String> = listOf("Départ : grotte + source, 10% connu.")
)

class Engine(private val safety: Boolean = true) {

    fun nextSeason(s: State): State {
        val assigned = assignRoles(s)
        val (afterActions, waterGain, fruitGain) = performActions(assigned)
        val afterResearch = research(afterActions)
        val afterDeposit = deposit(afterResearch, waterGain, fruitGain)
        val afterConsume = consumeAndAge(afterDeposit)
        val afterEvent = seasonalEvent(afterConsume)
        val logged = logTurn(afterEvent, waterGain, fruitGain)
        return advance(logged)
    }

    private fun assignRoles(s: State): State {
        val adults = s.people.filter { it.alive && it.age >= 18 }
        val need = if (safety) 0.20 else 0.25
        val low = (s.inv[Resource.WATER] ?: 0.0) < 2*need*adults.size || (s.inv[Resource.FRUITS] ?: 0.0) < 2*need*adults.size
        val roles = if (low) {
            adults.indices.map { Role.GATHER }
        } else {
            adults.indices.map { if (it == 0) Role.RESEARCH else Role.GATHER }
        }
        val newPeople = s.people.map { p ->
            if (!p.alive || p.age < 18) p else {
                val idx = adults.indexOfFirst { it.name == p.name }.coerceAtLeast(0)
                p.copy(role = roles[idx])
            }
        }
        return s.copy(people = newPeople)
    }

    private fun energy(p: Person): Double {
        val h = maxOf(0.2, p.hp / 100.0)
        val f = maxOf(0.0, (100 - p.fatigue) / 100.0)
        val m = (0.5 + 0.5 * (p.morale / 100.0)).coerceIn(0.5, 1.0)
        return h * f * m
    }

    private fun performActions(s: State): Triple<State, Double, Double> {
        var wGain = 0.0; var fGain = 0.0
        val seasonMult = when (s.season) { Season.WINTER->0.7; Season.SPRING->1.0; Season.SUMMER->1.2; Season.AUTUMN->1.1 }
        val area = s.tile.exploredPct
        val newP = s.people.map { p ->
            if (!p.alive) p else when (p.role) {
                Role.GATHER -> {
                    val base = 1.2 + 0.5 * p.gather
                    val e = energy(p)
                    val water = base*0.5*area*e*1.2*seasonMult
                    val fruits = (base*0.42*area*e*1.2*seasonMult).coerceAtLeast(0.02)
                    wGain += water; fGain += fruits; p
                }
                else -> p
            }
        }
        return Triple(s.copy(people = newP), wGain, fGain)
    }

    private fun research(s: State): State {
        val passive = 0.05
        val contrib = s.people.filter { it.role == Role.RESEARCH && it.alive }.sumOf {
            0.25 * 0.6 * energy(it) * (1.0 + 0.5 * it.observe)
        }
        val total = passive + contrib
        var obs = s.obs
        var fire = s.fire
        if (!obs.discovered) {
            val p = obs.progress + total
            obs = obs.copy(progress = p, discovered = p >= obs.complexity)
        } else if (!fire.discovered) {
            val p = fire.progress + total
            fire = fire.copy(progress = p, discovered = p >= fire.complexity)
        }
        val hearth = s.hasHearth || (fire.discovered)
        return s.copy(obs = obs, fire = fire, hasHearth = hearth)
    }

    private fun deposit(s: State, w: Double, f: Double): State {
        val inv = s.inv.toMutableMap()
        inv[Resource.WATER] = (inv[Resource.WATER] ?: 0.0) + w
        inv[Resource.FRUITS] = (inv[Resource.FRUITS] ?: 0.0) + f
        return s.copy(inv = inv)
    }

    private fun consumeAndAge(s: State): State {
        val need = if (safety) 0.20 else 0.25
        val inv = s.inv.toMutableMap()
        val newP = s.people.map { p ->
            if (!p.alive) p else {
                val water = inv[Resource.WATER] ?: 0.0
                val fruits = inv[Resource.FRUITS] ?: 0.0
                var hp = p.hp; var morale = p.morale
                if (water >= need) inv[Resource.WATER] = water - need else { hp = (hp-1).coerceAtLeast(0); morale = (morale-1).coerceAtLeast(0) }
                if (fruits >= need) inv[Resource.FRUITS] = fruits - need else { hp = (hp-1).coerceAtLeast(0); morale = (morale-1).coerceAtLeast(0) }
                val alive = hp > 0
                p.copy(hp = hp, morale = morale, age = p.age + 0.25, alive = alive)
            }
        }
        return s.copy(inv = inv, people = newP)
    }

    private fun seasonalEvent(s: State): State {
        val low = (s.inv[Resource.WATER] ?: 0.0) < 0.6
        return if (low) s.copy(inv = s.inv + (Resource.WATER to ((s.inv[Resource.WATER] ?: 0.0) + 0.6)),
            log = s.log + "🌧️ ${s.season}: Pluie (+0.6 eau)") else s
    }

    private fun logTurn(s: State, w: Double, f: Double): State {
        val line = "A${s.year} ${s.season} | Eau+${"%.2f".format(w)} Fruits+${"%.2f".format(f)} | Stocks E${"%.2f".format(s.inv[Resource.WATER])} F${"%.2f".format(s.inv[Resource.FRUITS])} | OBS ${"%.1f".format(s.obs.progress)}/${s.obs.complexity} FIRE ${"%.1f".format(s.fire.progress)}/${s.fire.complexity}"
        return s.copy(log = (s.log + line).takeLast(200))
    }

    private fun advance(s: State): State {
        val nextSeason = Season.values()[(s.season.ordinal + 1) % 4]
        val nextYear = if (nextSeason == Season.WINTER) s.year + 1 else s.year
        return s.copy(season = nextSeason, year = nextYear)
    }
}
