package pathfinder.bankingBot.banking.jpa

import jakarta.persistence.AttributeConverter

enum class TransactionType {
    RECEIVE, DEPOSIT, INTEREST;

    class Converter: AttributeConverter<Set<TransactionType>, String> {
        override fun convertToDatabaseColumn(attribute: Set<TransactionType>) =
            attribute.joinToString(", ") { it.name }

        override fun convertToEntityAttribute(dbData: String) =
            dbData.split(", ").map(::valueOf).toSet()
    }
}