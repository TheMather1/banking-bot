package pathfinder.bankingBot.banking.jpa

import jakarta.persistence.*
import jakarta.persistence.GenerationType.IDENTITY
import net.dv8tion.jda.api.EmbedBuilder
import pathfinder.bankingBot.banking.jpa.Frequency.MONTHLY
import pathfinder.diceSyntax.DiceParser
import java.io.Serializable

@Entity
@Table(name = "ACCOUNT_TYPES")
class AccountTypeEntity(
    @Id
    @GeneratedValue(strategy = IDENTITY)
    val id: Long,
    @ManyToOne
    @JoinColumn(name = "BANK_ID")
    val bank: BankEntity,
    var name: String,
    var interestRate: String,
    @Enumerated(EnumType.STRING)
    var frequency: Frequency = MONTHLY
): Serializable {
    fun asEmbed(numAccounts: Int) = EmbedBuilder().setTitle(name).addField("Interest rate", "$interestRate%", false)
        .addField("Interest frequency", frequency.name.lowercase().replaceFirstChar { it.uppercase() }, false)
        .setFooter("$numAccounts accounts of type").build()

    @get:Transient
    val interestPercent
        get() = DiceParser().parse(interestRate).toDouble()

    override fun toString() = name
}
