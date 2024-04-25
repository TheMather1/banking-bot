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
    @OneToMany(fetch = EAGER, cascade = [ALL], mappedBy = "bank")
//    @LazyCollection(LazyCollectionOption.FALSE)
    val characters: MutableList<CharacterEntity> = mutableListOf(),
    @OneToMany(fetch = EAGER, cascade = [ALL], mappedBy = "bank")
//    @LazyCollection(LazyCollectionOption.FALSE)
    val accountTypes: MutableList<AccountTypeEntity> = mutableListOf(),
    var logChannel: Long? = null
): Serializable {
    @Transient
    lateinit var jda: JDA
}
