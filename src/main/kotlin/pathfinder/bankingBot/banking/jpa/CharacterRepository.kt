package pathfinder.bankingBot.banking.jpa

import org.springframework.data.jpa.repository.JpaRepository

interface CharacterRepository: JpaRepository<CharacterEntity, Long> {

    fun getByBank_IdAndPlayerId(bank_id: Long, playerId: Long?): List<CharacterEntity>

    fun findAllByBank_Id(bankId: Long): List<CharacterEntity>

    fun findAllByAccounts_AccountTypeNot(accountTypeEntity: AccountTypeEntity): List<CharacterEntity>


}