package pathfinder.bankingBot.banking.jpa

import jakarta.persistence.*
import jakarta.persistence.CascadeType.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import pathfinder.bankingBot.NUMBER_FORMAT
import pathfinder.bankingBot.banking.Denomination
import pathfinder.bankingBot.banking.Paginatable
import pathfinder.bankingBot.banking.TransactionType
import pathfinder.bankingBot.banking.TransactionType.*
import pathfinder.bankingBot.truncateToCopper

@Entity
@Table(name = "ACCOUNTS")
class AccountEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne
    @JoinColumn(name = "CHARACTER_ID", nullable = false)
    var character: CharacterEntity,
    @ManyToOne
    @JoinColumn(name = "ACCOUNT_TYPE_ID", nullable = false)
    val accountType: AccountTypeEntity,
    @OneToMany(mappedBy = "account", fetch = FetchType.EAGER, cascade = [PERSIST, MERGE, REFRESH, DETACH])
    val logs: MutableList<LogEntity> = mutableListOf(),
    @Column(nullable = false, scale = 2)
    var balance: Double = 0.0
) : Paginatable {

    override fun toString() = "$accountType"

    fun fullName() = "$character - $accountType"

    fun deposit(value: Double, actor: User? = null) {
        val tValue = tax(value, DEPOSIT)
        balance += tValue
        if (actor == null) log("$character deposited ${format(tValue)} GP.")
        else  log("${actor.effectiveName} deposited ${format(tValue)} GP.")
    }

    fun earn(value: Double, denomination: Denomination, activity: String, roll: String) {
        val tValue = tax(denomination(value), EARN)
        balance += tValue
        log("$character earned $tValue GP $activity. [$roll $denomination]")
    }

    fun withdraw(value: Double, actor: User? = null) {
        val tValue = truncateToCopper(value)
        balance -= tValue
        if (actor == null) log("$character withdrew ${format(tValue)} GP.")
        else  log("${actor.effectiveName} withdrew ${format(tValue)} GP.")
    }

    fun send(value: Double, recipient: AccountEntity) {
        val tValue = truncateToCopper(value)
        balance -= tValue
        log("Sent ${format(tValue)} GP to ${recipient.fullName()}.")
    }

    fun receive(value: Double, sender: AccountEntity) {
        val tValue = tax(value, RECEIVE)
        balance += tValue
        log("Received ${format(tValue)} GP from ${sender.fullName()}.")
    }

    fun receiveTax(value: Double, sender: AccountEntity) {
        val tValue = truncateToCopper(value)
        balance += tValue
        log("Received ${format(tValue)} GP in tax from ${sender.fullName()}.")
    }

    fun interest() {
        val tValue = tax(accountType.interestPercent/100 * balance, INTEREST)
        balance = truncateToCopper(balance + tValue)
        log("Gained ${format(tValue)} GP in interest.")
    }

    fun set(value: Double, actor: User) {
        val tValue = truncateToCopper(value)
        balance = tValue
        log("${actor.effectiveName} set balance to ${format(tValue)} GP.")
    }

    private fun log(description: String) {
        logs.add(LogEntity(0, this, description, balance))
    }

    private fun format(double: Double) = NUMBER_FORMAT.format(double)

    private fun tax(value: Double, type: TransactionType) = accountType.taxConfig?.tax(value, type, this) ?: truncateToCopper(value)

    override fun asEmbed() = EmbedBuilder().setTitle(character.name).addField("Type", accountType.name, true)
    .addField("Balance", "${format(balance)} GP", true).build()
}
