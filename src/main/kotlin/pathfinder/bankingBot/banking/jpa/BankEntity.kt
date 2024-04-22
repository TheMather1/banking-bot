package pathfinder.bankingBot.banking.jpa

import net.dv8tion.jda.api.JDA
import org.hibernate.annotations.LazyCollection
import org.hibernate.annotations.LazyCollectionOption
import java.io.Serializable
import javax.persistence.CascadeType.ALL
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "BANKS")
class BankEntity(
    @Id
    val id: Long,
    @OneToMany(cascade = [ALL])
    @LazyCollection(LazyCollectionOption.FALSE)
    val characters: MutableList<CharacterEntity> = mutableListOf(),
    @OneToMany(cascade = [ALL])
    @LazyCollection(LazyCollectionOption.FALSE)
    val accountTypes: MutableList<AccountTypeEntity> = mutableListOf(),
    var logChannel: Long? = null
): Serializable {
    @Transient
    lateinit var jda: JDA
}
