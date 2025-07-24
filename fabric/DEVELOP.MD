## Fabric Development
This mod is using [Stonecutter](https://stonecutter.kikugie.dev/) to support multiple Minecraft versions.
To add a new feature in this mod you need to follow these steps:
1. Run gradle task "Set active project to X" in stonecutter where X is the lowest Minecraft version you want to support.
2. Go to `fabric/scripts` and run the script for your operating system to set up the mapped sources
3. Go to `fabric/versions/<version>/src` and add your changes there as intelij idea will not add the correct classpath to the main sources folder. 
4. Make sure that whatever api you are using is available in all Fabric and Minecraft versions. 
5. Before you commit your changes, run the gradle task "Reset active project" in stonecutter

## Adventure API
This mod uses all 3 major versions of adventure-platform-fabric (4, 5 and 6) and the lowest Minecraft version supported due to this is 1.16.2.
You can use the following examples to see how the checks are done in the code: 
```java
//? if adventure: <6 {
/*@SuppressWarnings("removal")
Component component = discordSRV.getAdventure().toAdventure(signedMessage.getContent());
*///?} else {
Component component = discordSRV.getAdventure().asAdventure(signedMessage.getContent());
 //?}
```
```java
//? if adventure: >=5.11.0 {
return player.identity();
//?} else {
/*return Identity.identity(player.getUuid());
*///?}
```

## Mixins
You may want to use mixins in the case that a fabric api is not supported in the lowest Minecraft version.
Refer to CommandManagerMixin and FabricRequiredLinkingModule for an example of how to use mixins in this mod with changing Minecraft classes.