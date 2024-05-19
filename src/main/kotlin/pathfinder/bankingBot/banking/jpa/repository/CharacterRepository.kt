package pathfinder.bankingBot.banking.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pathfinder.bankingBot.banking.jpa.AccountTypeEntity
import pathfinder.bankingBot.banking.jpa.CharacterEntity

@Repository
interface CharacterRepository: JpaRepository<CharacterEntity, Long> {

    fun getByBank_IdAndPlayerId(bank_id: Long, playerId: Long?): List<CharacterEntity>

    fun findAllByBank_Id(bankId: Long): List<CharacterEntity>

    fun findAllByAccounts_AccountTypeNot(accountTypeEntity: AccountTypeEntity): List<CharacterEntity>

//    @Modifying
//    @Query("delete from CharacterEntity where id = ?1")
//    override fun deleteById(id: Long)
}