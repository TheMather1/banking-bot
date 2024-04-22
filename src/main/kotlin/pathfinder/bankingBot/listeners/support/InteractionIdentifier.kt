package pathfinder.bankingBot.listeners.support

import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.utils.data.SerializableData

interface InteractionIdentifier<T: IReplyCallback> : (SerializableData, Interaction) -> Boolean