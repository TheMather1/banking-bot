package pathfinder.bankingBot.banking.jpa

import net.dv8tion.jda.api.EmbedBuilder
import pathfinder.bankingBot.banking.jpa.Frequency.MONTHLY
import java.io.Serializable
import javax.persistence.*
import javax.persistence.GenerationType.IDENTITY

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
    fun asEmbed(numAccounts: Int) = EmbedBuilder().setTitle(name).addField("Interest rate", interestRate, false)
        .addField("Interest frequency", frequency.name.lowercase().replaceFirstChar { it.uppercase() }, false)
        .setFooter("$numAccounts accounts of type").build()

    override fun toString() = name
}
