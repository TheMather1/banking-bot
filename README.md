A bot for managing banking services for living world tabletop RPGS.

You can add the bot to your server [here](https://discord.com/oauth2/authorize?client_id=989813183374032946) *The bot is under development and sporadically hosted on my local computer. I make no promises for uptime or data persistence.*

## Basics
This bot allows the use of persistent money management, in the standard fantasy tabletop units of Copper Pieces(CP), Silver Pieces(SP), Gold Pieces(GP), and Platinum Pieces(PP).

Money is held by characters, and divided into accounts.

Each account type has a configurable rate of interest, which may be applied daily, weekly, monthly, or never.

Similarly each account type can be configured to pay taxes on its income, to a specified non-player account, with configuration as to whether the tax should be deducted from the income.

Additionally each character has a wallet. This "account" has no interest rate, and cannot be taxed. The bank has no authority over physical currency.

## Commands
Permission to use commands will need to be granted under APPS > Integrations in your server settings.

### Management
* **Manage Characters** *(context command)* - Add, delete, or edit player characters and their accounts.
* **npc_accounts** *(slash command)* - Add, delete, or edit non-player characters and their accounts.
* **account_types** *(slash command)* - Add, delete, or edit account types and their interest rates for the server.
* **downtime_config** *(slash command)* - Edit the server's default configuration for downtime earnings.
* **log_channel** *(slash command)* - Assign the text channel for the bot to log bank events to.
* **tax_config** *(slash command)* - Add, delete or, edit tax rates for account types, and which account the taxes go to.
### DM commands
* **Send money from NPC** *(context command)* - Send money from an NPC to a player character.
### Player commands
* **Send money** *(context command)* - Send money from your own accounts to another player.
* **npc_send** *(slash command)* - Send money from your own accounts to an NPC.
* **bank** *(slash command)* - View your accounts and deposit or withdraw money from them.
* **downtime** *(slash command)* - 

#### Glossary
context command - Commands performed by right-clicking server members, under Apps
slash command - Commands performed by typing `/` followed by the command.