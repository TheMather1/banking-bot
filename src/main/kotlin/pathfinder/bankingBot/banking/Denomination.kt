package pathfinder.bankingBot.banking

enum class Denomination(private val multiplier: Double) {
    CP(0.01),
    SP(0.1),
    GP(1.0),
    PP(10.0);

    infix operator fun invoke(x: Double): Double = x*multiplier
}