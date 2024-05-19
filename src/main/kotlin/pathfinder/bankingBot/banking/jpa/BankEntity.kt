package pathfinder.bankingBot.banking.jpa

import jakarta.persistence.*
import jakarta.persistence.CascadeType.ALL
import jakarta.persistence.FetchType.EAGER
import net.dv8tion.jda.api.JDA
import java.io.Serializable

@Entity
@Table(name = "BANKS")
class BankEntity(
    @Id
    val id: Long,
    @OneToMany(fetch = EAGER, cascade = [ALL], mappedBy = "bank", orphanRemoval = true)
//    @LazyCollection(LazyCollectionOption.FALSE)
    val characters: MutableList<CharacterEntity> = mutableListOf(),
    @OneToMany(fetch = EAGER, cascade = [ALL], mappedBy = "bank", orphanRemoval = true)
//    @LazyCollection(LazyCollectionOption.FALSE)
    val accountTypes: MutableList<AccountTypeEntity> = mutableListOf(),
    var logChannel: Long? = null,
): Serializable {
    @OneToOne(fetch = EAGER, cascade = [ALL], mappedBy = "bank")
    val downtimeConfig: DowntimeConfig = DowntimeConfig(0, this)

    @Transient
    lateinit var jda: JDA
}
