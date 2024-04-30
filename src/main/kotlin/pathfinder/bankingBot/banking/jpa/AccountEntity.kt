package pathfinder.bankingBot.banking.jpa

import jakarta.persistence.*
import jakarta.persistence.CascadeType.ALL
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User
import pathfinder.bankingBot.banking.jpa.TransactionType.*
import pathfinder.bankingBot.truncateToCopper

@Entity
@Table(name = "ACCOUNTS")
class AccountEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne
    @JoinColumn(name = "CHARACTER_ID", nullable = false)
    val character: CharacterEntity,
    @ManyToOne
    @JoinColumn(name = "ACCOUNT_TYPE_ID", nullable = false)
    val accountType: AccountTypeEntity,
    @OneToMany(targetEntity = LogEntity::class, mappedBy = "account", fetch = FetchType.EAGER, cascade = [ALL])
    val logs: MutableList<LogEntity> = mutableListOf(),
    @Column(nullable = false)
    var balance: Double = 0.0
) : Paginatable {

    override fun toString() = "$accountType"

    fun fullName() = "$character - $accountType"

    fun deposit(value: Double, actor: User? = null) {
        val tValue = tax(value, DEPOSIT)
        balance += tValue
        if (actor == null) log("$character deposited $tValue.")
        else  log("${actor.effectiveName} deposited $tValue.")
    }

    fun withdraw(value: Double, actor: User? = null) {
        val tValue = truncateToCopper(value)
        balance -= tValue
        if (actor == null) log("$character withdrew $tValue.")
        else  log("${actor.effectiveName} withdrew $tValue.")
    }

    fun send(value: Double, recipient: AccountEntity) {
        val tValue = truncateToCopper(value)
        balance -= tValue
        log("Sent $tValue to ${recipient.fullName()}.")
    }

    fun receive(value: Double, sender: AccountEntity) {
        val tValue = tax(value, RECEIVE)
        balance += tValue
        log("Received $tValue from ${sender.fullName()}.")
    }

    fun receiveTax(value: Double, sender: AccountEntity) {
        val tValue = truncateToCopper(value)
        balance += tValue
        log("Received $tValue in tax from ${sender.fullName()}.")
    }

    fun interest() {
        val tValue = tax(accountType.interestPercent/100 * balance, INTEREST)
        balance = truncateToCopper(balance + tValue)
        log("Gained $tValue in interest.")
    }

    fun set(value: Double, actor: User) {
        val tValue = truncateToCopper(value)
        balance = tValue
        log("${actor.effectiveName} set balance to $value.")
    }

    private fun log(description: String) {
        logs.add(LogEntity(0, this, description, balance))
    }

    private fun tax(value: Double, type: TransactionType) = truncateToCopper(accountType.taxConfig?.tax(value, type, this) ?: value)

    override fun asEmbed() = EmbedBuilder().setTitle(character.name).addField("Type", accountType.name, true)
    .addField("Balance", "$balance gp", true).build()
}
