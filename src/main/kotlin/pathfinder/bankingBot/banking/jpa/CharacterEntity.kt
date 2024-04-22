package pathfinder.bankingBot.banking.jpa

import net.dv8tion.jda.api.EmbedBuilder
import javax.persistence.*
import javax.persistence.CascadeType.ALL
import javax.persistence.FetchType.EAGER

@Entity
@Table(name = "CHARACTERS")
class CharacterEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne
    @JoinColumn(name = "BANK_ID", nullable = false)
    val bank: BankEntity,
    @Column(nullable = true)
    val playerId: Long?,
    val name: String,
    @OneToMany(fetch = EAGER, cascade = [ALL])
    val accounts: MutableList<AccountEntity> = mutableListOf()
) : Paginatable {
    override fun asEmbed() = EmbedBuilder().setTitle(name).apply {
        accounts.forEach {
            addField(it.accountType.name, "${it.balance} gp", false)
        }
        if (accounts.isEmpty()) setDescription("No accounts.")
    }.build()

    override fun toString() = name
}
