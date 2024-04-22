package pathfinder.bankingBot.banking.jpa

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import javax.persistence.*
import javax.persistence.CascadeType.ALL
import kotlin.math.floor

@Entity
@Table(name = "PLAYER_ACCOUNTS")
class AccountEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    val id: Long,
    @ManyToOne
    @JoinColumn(name = "CHARACTER_ID")
    val character: CharacterEntity,
    @ManyToOne
    @JoinColumn(name = "ACCOUNT_TYPE_ID")
    val accountType: AccountTypeEntity,
    @OneToMany(targetEntity = LogEntity::class, mappedBy = "account", fetch = FetchType.EAGER, cascade = [ALL])
    val logs: MutableList<LogEntity> = mutableListOf(),
    var balance: Double = 0.0
) : Paginatable {

    override fun toString() = "$accountType"

    fun fullName() = "$character - $accountType"

    fun deposit(value: Double, actor: User? = null) {
        val tValue = truncateToCopper(value)
        balance += tValue
        if (actor == null) log("$character deposited $tValue.")
        else  log("${actor.effectiveName} deposited $tValue.")
//        return Transaction(DEPOSIT, tValue, this, actor)
    }

    fun withdraw(value: Double, actor: User? = null) {
        val tValue = truncateToCopper(value)
        balance -= tValue
        if (actor == null) log("$character withdrew $tValue.")
        else  log("${actor.effectiveName} withdrew $tValue.")
//        return Transaction(WITHDRAW, tValue, actor, this)
    }

    fun send(value: Double, recipient: AccountEntity) {
        val tValue = truncateToCopper(value)
        balance -= tValue
        log("Sent $tValue to ${recipient.character} - $recipient.")
//        return Transaction(TRANSFER, tValue, recipient, this)
    }

    fun receive(value: Double, sender: AccountEntity) {
        val tValue = truncateToCopper(value)
        balance += tValue
        log("Received $tValue from ${sender.character} - $sender.")
//        return Transaction(TRANSFER, tValue, recipient, this)
    }

    fun interest() {
        val tValue = truncateToCopper(accountType.interestRate.toDouble()/100 * balance)
        balance += tValue
        log("Gained $tValue in interest.")
//        return Transaction(INTEREST, tValue, this, this)
    }

    fun set(value: Double, actor: User) {
        val tValue = truncateToCopper(value)
        balance = tValue
        log("${actor.effectiveName} set balance to $value.")
        //return Transaction(SET, tValue, this, actor)
    }

    private fun log(description: String) {
        logs.add(LogEntity(0, this, description))
    }

    override fun asEmbed() = EmbedBuilder().setTitle(character.name).addField("Type", accountType.name, true)
    .addField("Balance", "$balance gp", true).build()

    private fun truncateToCopper(value: Double) = floor(value * 100) / 100
}
