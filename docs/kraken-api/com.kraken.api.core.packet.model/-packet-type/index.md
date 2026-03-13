//[kraken-api](../../../index.md)/[com.kraken.api.core.packet.model](../index.md)/[PacketType](index.md)

# PacketType

[Kraken API]\
enum [PacketType](index.md)

Enum containing references to various packet types sent by the game client. 

 - IF Button types are sent when any of the normal buttons on newer interfaces are clicked - Resume pause are sent when a player interacts with a Dialogue like talking to an NPC &quot;Click here to Continue&quot; - Resume counts are sent when the dialogue asks for a number like in clue scroll steps - Resume string is sent when the dialogue asks for a string like &quot;whats your clan name&quot;? - Move game click is the packet that the client sends upon clicking on a game square to move towards it. - Event Mouse click packets are written whenever the player clicks anywhere on their client, whether it be dead space, or any entity in-game

## Entries

| | |
|---|---|
| [OPHELDD](-o-p-h-e-l-d-d/index.md) | [Kraken API]<br>[OPHELDD](-o-p-h-e-l-d-d/index.md) |
| [RESUME_COUNTDIALOG](-r-e-s-u-m-e_-c-o-u-n-t-d-i-a-l-o-g/index.md) | [Kraken API]<br>[RESUME_COUNTDIALOG](-r-e-s-u-m-e_-c-o-u-n-t-d-i-a-l-o-g/index.md) |
| [RESUME_PAUSEBUTTON](-r-e-s-u-m-e_-p-a-u-s-e-b-u-t-t-o-n/index.md) | [Kraken API]<br>[RESUME_PAUSEBUTTON](-r-e-s-u-m-e_-p-a-u-s-e-b-u-t-t-o-n/index.md) |
| [RESUME_NAMEDIALOG](-r-e-s-u-m-e_-n-a-m-e-d-i-a-l-o-g/index.md) | [Kraken API]<br>[RESUME_NAMEDIALOG](-r-e-s-u-m-e_-n-a-m-e-d-i-a-l-o-g/index.md) |
| [RESUME_STRINGDIALOG](-r-e-s-u-m-e_-s-t-r-i-n-g-d-i-a-l-o-g/index.md) | [Kraken API]<br>[RESUME_STRINGDIALOG](-r-e-s-u-m-e_-s-t-r-i-n-g-d-i-a-l-o-g/index.md) |
| [RESUME_OBJDIALOG](-r-e-s-u-m-e_-o-b-j-d-i-a-l-o-g/index.md) | [Kraken API]<br>[RESUME_OBJDIALOG](-r-e-s-u-m-e_-o-b-j-d-i-a-l-o-g/index.md) |
| [IF_BUTTON](-i-f_-b-u-t-t-o-n/index.md) | [Kraken API]<br>[IF_BUTTON](-i-f_-b-u-t-t-o-n/index.md) |
| [IF_SUBOP](-i-f_-s-u-b-o-p/index.md) | [Kraken API]<br>[IF_SUBOP](-i-f_-s-u-b-o-p/index.md) |
| [IF_BUTTONX](-i-f_-b-u-t-t-o-n-x/index.md) | [Kraken API]<br>[IF_BUTTONX](-i-f_-b-u-t-t-o-n-x/index.md) |
| [OPNPC](-o-p-n-p-c/index.md) | [Kraken API]<br>[OPNPC](-o-p-n-p-c/index.md) |
| [OPPLAYER](-o-p-p-l-a-y-e-r/index.md) | [Kraken API]<br>[OPPLAYER](-o-p-p-l-a-y-e-r/index.md) |
| [OPOBJ](-o-p-o-b-j/index.md) | [Kraken API]<br>[OPOBJ](-o-p-o-b-j/index.md) |
| [OPLOC](-o-p-l-o-c/index.md) | [Kraken API]<br>[OPLOC](-o-p-l-o-c/index.md) |
| [MOVE_GAMECLICK](-m-o-v-e_-g-a-m-e-c-l-i-c-k/index.md) | [Kraken API]<br>[MOVE_GAMECLICK](-m-o-v-e_-g-a-m-e-c-l-i-c-k/index.md) |
| [EVENT_MOUSE_CLICK](-e-v-e-n-t_-m-o-u-s-e_-c-l-i-c-k/index.md) | [Kraken API]<br>[EVENT_MOUSE_CLICK](-e-v-e-n-t_-m-o-u-s-e_-c-l-i-c-k/index.md) |
| [IF_BUTTONT](-i-f_-b-u-t-t-o-n-t/index.md) | [Kraken API]<br>[IF_BUTTONT](-i-f_-b-u-t-t-o-n-t/index.md) |
| [OPNPCT](-o-p-n-p-c-t/index.md) | [Kraken API]<br>[OPNPCT](-o-p-n-p-c-t/index.md) |
| [OPPLAYERT](-o-p-p-l-a-y-e-r-t/index.md) | [Kraken API]<br>[OPPLAYERT](-o-p-p-l-a-y-e-r-t/index.md) |
| [OPOBJT](-o-p-o-b-j-t/index.md) | [Kraken API]<br>[OPOBJT](-o-p-o-b-j-t/index.md) |
| [OPLOCT](-o-p-l-o-c-t/index.md) | [Kraken API]<br>[OPLOCT](-o-p-l-o-c-t/index.md) |
| [SET_HEADING](-s-e-t_-h-e-a-d-i-n-g/index.md) | [Kraken API]<br>[SET_HEADING](-s-e-t_-h-e-a-d-i-n-g/index.md) |

## Functions

| Name | Summary |
|---|---|
| [forMenuAction](for-menu-action.md) | [Kraken API]<br>open fun [forMenuAction](for-menu-action.md)(action: MenuAction): [PacketType](index.md)<br>Makes a best effort approach to mapping a RuneLite menu action for a game click to the underlying packet this that is sent to the server |
| [getParams](get-params.md) | [Kraken API]<br>open fun [getParams](get-params.md)(): [List](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/List.html)&lt;[String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)&gt;<br>Retrieves a list of parameter names required for the current @PacketType. |
| [valueOf](value-of.md) | [Kraken API]<br>open fun [valueOf](value-of.md)(name: [String](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/lang/String.html)): [PacketType](index.md)<br>Returns the enum constant of this type with the specified name. The string must match exactly an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.) |
| [values](values.md) | [Kraken API]<br>open fun [values](values.md)(): [Array](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-array/index.html)&lt;[PacketType](index.md)&gt;<br>Returns an array containing the constants of this enum type, in the order they're declared. This method may be used to iterate over the constants. |
