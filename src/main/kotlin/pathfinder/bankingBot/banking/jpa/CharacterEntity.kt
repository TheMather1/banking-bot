package pathfinder.bankingBot.banking.jpa

import jakarta.persistence.*
import jakarta.persistence.CascadeType.ALL
import jakarta.persistence.FetchType.EAGER
import net.dv8tion.jda.api.EmbedBuilder
import pathfinder.bankingBot.NUMBER_FORMAT
import pathfinder.bankingBot.banking.Paginatable

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
    @Column(nullable = false)
    val name: String,
    @OneToMany(fetch = EAGER, cascade = [ALL], mappedBy = "character", orphanRemoval = true)
    val accounts: MutableList<AccountEntity> = mutableListOf()
) : Paginatable {
    @OneToOne(fetch = EAGER, cascade = [ALL], mappedBy = "character", orphanRemoval = true)
    val downtimeOverride: DowntimeOverride = DowntimeOverride(0, this)

    override fun asEmbed() = EmbedBuilder().setTitle(name).apply {
        accounts.forEach {
            addField(it.accountType.name, "${NUMBER_FORMAT.format(it.balance)} gp", false)
        }
        if (accounts.isEmpty()) setDescription("No accounts.")
    }.build()

    fun getAccountById(id: Long) = accounts.firstOrNull { it.id == id }
    fun getAccountById(id: String) = accounts.firstOrNull { it.id.toString() == id }

    fun hasAccount(type: AccountTypeEntity) = accounts.any { it.accountType.id == type.id }
    fun getAccountByType(type: AccountTypeEntity) = accounts.firstOrNull { it.accountType.id == type.id }

    override fun toString() = name
}
