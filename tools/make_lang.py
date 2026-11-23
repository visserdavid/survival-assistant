"""Writes en_us.json and nl_nl.json for Survival Assistant with identical key sets.

Usage: python tools/make_lang.py src/main/resources/assets/survivalassistant/lang

English and Dutch live in the table below; other languages in tools/lang/<code>.json (key -> text).
Every language must have exactly the English keys and the same number of %s placeholders.
"""
import glob
import json
import os
import re
import sys
import unicodedata

P = "survivalassistant."
T = P + "tip."

# key: (english, dutch)
L = {
    "entity.survivalassistant.guide": ("Guide", "Gids"),

    P + "welcome.1": ("Hello %s! I'm your guide. I'll stay with you for your first days and help you survive.",
                      "Hoi %s! Ik ben je gids. Ik blijf de eerste dagen bij je en help je overleven."),
    P + "welcome.2": ("Sneak and right-click me for a tip, or use /guide tip. Want some peace and quiet? /guide mute <minutes>.",
                      "Sluip en rechtsklik op mij voor een tip, of gebruik /guide tip. Even rust? /guide mute <minuten>."),
    P + "comfort.1": ("Ouch! Don't worry: your items stay at the spot where you died for 5 minutes. Go get them, carefully!",
                      "Au! Geen paniek: je spullen blijven 5 minuten liggen waar je doodging. Haal ze op, maar voorzichtig!"),
    P + "comfort.2": ("That happens to everyone. Your items are still lying where you fell, for 5 minutes. Hurry, but stay safe.",
                      "Dat overkomt iedereen. Je spullen liggen nog 5 minuten op de plek waar je viel. Snel, maar veilig."),
    P + "chat.unavailable": ("I seem to have lost my voice. Talk to me again in a minute!",
                             "Ik ben even mijn stem kwijt. Praat over een minuutje nog eens met me!"),
    P + "all_done": ("You've done everything I can teach you. You're ready for the world!",
                     "Je hebt alles gedaan wat ik je kan leren. Je bent klaar voor de wereld!"),
    P + "progress": ("Goals reached: %s/%s. I'll stay with you for %s more day(s).",
                     "Doelen gehaald: %s/%s. Ik blijf nog %s dag(en) bij je."),
    P + "farewell.1": ("You made it through your first days! It's time for me to move on.",
                       "Je hebt je eerste dagen overleefd! Tijd voor mij om verder te trekken."),
    P + "dismissed.1": ("Alright, I'll leave you to it. Good luck out there!",
                        "Goed, ik laat je verder alleen. Veel succes daarbuiten!"),
    P + "farewell.summary": ("You reached %s of %s goals. Keep exploring, and always sleep at night.",
                             "Je haalde %s van de %s doelen. Blijf ontdekken en slaap elke nacht."),
    P + "farewell.recall": ("Need me again? Use /guide call.", "Toch nog hulp nodig? Gebruik /guide call."),
    P + "recall": ("I'm here! What can I help you with?", "Ik ben er! Waarmee kan ik je helpen?"),
    P + "extend.ask": ("Our days together are over. Do you still need me?",
                       "Onze dagen samen zitten erop. Heb je mij nog nodig?"),
    P + "extend.reminder": ("Just checking: do you still want me around?",
                            "Even checken: wil je dat ik nog blijf?"),
    P + "extend.yes": ("Yes, %s more days", "Ja, %s dagen extra"),
    P + "extend.no": ("No, thanks", "Nee, bedankt"),
    P + "extend.yes_hover": ("The guide stays %s more days (/guide extend)",
                             "De gids blijft nog %s dagen (/guide extend)"),
    P + "extend.no_hover": ("The guide says goodbye (/guide dismiss)",
                            "De gids neemt afscheid (/guide dismiss)"),
    P + "extend.confirmed": ("Great, I'll stay with you for %s more day(s)!",
                             "Top, ik blijf nog %s dag(en) bij je!"),
    P + "extend.no_answer.1": ("I didn't hear from you, so I think you're doing fine on your own. Time for me to go!",
                               "Ik hoorde niets meer, dus je redt je vast prima alleen. Tijd voor mij om te gaan!"),
    P + "congrats.1": ("Congratulations! You reached every goal. You're a real survivor now!",
                       "Gefeliciteerd! Je hebt alle doelen gehaald. Je bent nu een echte overlever!"),
    P + "congrats.2": ("You don't need me anymore. Keep exploring, and always sleep at night.",
                       "Je hebt mij niet meer nodig. Blijf ontdekken en slaap elke nacht."),
    P + "congrats.title": ("Congratulations!", "Gefeliciteerd!"),
    P + "congrats.subtitle": ("All %s goals reached", "Alle %s doelen gehaald"),
    P + "chapter2.offer": ("Want to keep going? Chapter 2: a farm, full armor, diamonds and getting ready for the Nether.",
                           "Wil je verder? Hoofdstuk 2: een akker, een volledig harnas, diamanten en klaarmaken voor de Nether."),
    P + "chapter2.reminder": ("Still thinking about chapter 2? Click a button below.",
                              "Nog aan het nadenken over hoofdstuk 2? Klik op een knop hieronder."),
    P + "chapter2.yes": ("Yes, chapter 2", "Ja, hoofdstuk 2"),
    P + "chapter2.yes_hover": ("Start chapter 2 (/guide continue)", "Begin hoofdstuk 2 (/guide continue)"),
    P + "chapter2.start": ("Chapter 2 begins! I'll stay with you for %s days. First up: a farm.",
                           "Hoofdstuk 2 begint! Ik blijf %s dagen bij je. Eerst: een akker."),
    P + "chapter2.no_answer.1": ("I didn't hear from you, so I'll let you continue on your own. You're ready!",
                                 "Ik hoorde niets meer, dus ik laat je verder alleen. Je bent er klaar voor!"),
    P + "congrats.chapter2.1": ("Congratulations! You finished chapter 2. You're ready for the Nether and beyond!",
                                "Gefeliciteerd! Je hebt hoofdstuk 2 af. Je bent klaar voor de Nether en verder!"),
    P + "congrats.chapter2.2": ("Build your portal when you're ready, and bring food, armor and a bow.",
                                "Bouw je portaal als je er klaar voor bent, en neem eten, een harnas en een boog mee."),
    P + "cmd.muted": ("The guide will stay quiet for %s minute(s). Sneak + right-click or /guide tip still works.",
                      "De gids is %s minuut/minuten stil. Sluipen + rechtsklik of /guide tip werkt nog wel."),
    P + "cmd.unavailable": ("Your guide isn't available right now.", "Je gids is nu niet beschikbaar."),

    # ---------------- urgent ----------------
    T + "creeper.1": ("A creeper is close! Run away right now. It explodes about 1.5 seconds after hissing.",
                      "Er is een creeper vlakbij! Ren meteen weg. Hij ontploft zo'n 1,5 seconde na het sissen."),
    T + "creeper.2": ("Creeper! Keep your distance: put at least 16 blocks between you, or a block between you and it.",
                      "Creeper! Hou afstand: minstens 16 blokken, of zet een blok tussen jou en hem."),
    T + "danger.1": ("You're badly hurt and monsters are near. Back off and put blocks between you and them!",
                     "Je bent zwaar gewond en er zijn monsters in de buurt. Trek je terug en zet blokken tussen jou en hen!"),
    T + "danger.2": ("Low health! Don't fight now. Dig into a wall, seal the hole and eat to recover.",
                     "Weinig levens! Ga nu niet vechten. Graaf je in een wand, dicht het gat en eet om te herstellen."),
    T + "hungry_eat.1": ("You're hungry: you can't sprint anymore. Eat something now!",
                         "Je hebt honger: je kunt niet meer sprinten. Eet nu iets!"),
    T + "hungry_eat.2": ("Eat! You only heal by yourself while your hunger bar is almost full.",
                         "Eten! Je geneest alleen vanzelf als je hongerbalk bijna vol is."),
    T + "hungry_hunt.1": ("You're hungry and have no food. Hunt cows, pigs, sheep or chickens, and cook the meat.",
                          "Je hebt honger en geen eten. Jaag op koeien, varkens, schapen of kippen en bak het vlees."),
    T + "hungry_hunt.2": ("No food left! Look for animals, or pick apples from oak leaves. Don't let your hunger reach zero.",
                          "Geen eten meer! Zoek dieren, of pluk appels uit eikenbladeren. Laat je honger niet op nul komen."),
    T + "dusk.1": ("The sun is setting! Get inside now, or quickly dig into a hill and close the hole behind you.",
                   "De zon gaat onder! Ga nu naar binnen, of graaf je snel in een heuvel en dicht het gat achter je."),
    T + "dusk.2": ("Evening is coming. Monsters spawn in the dark: find shelter or sleep in your bed.",
                   "Het wordt avond. Monsters verschijnen in het donker: zoek beschutting of slaap in je bed."),
    T + "dusk.3": ("Almost night! No shelter? Dig three blocks down, stand in the hole and place a block above you.",
                   "Bijna nacht! Geen schuilplaats? Graaf drie blokken omlaag, ga in het gat staan en zet een blok boven je."),
    T + "night_outside.1": ("It's night and you're outside. Monsters are everywhere: go inside or dig in!",
                            "Het is nacht en je bent buiten. Overal monsters: ga naar binnen of graaf je in!"),
    T + "night_outside.2": ("Stay out of the dark! Seal yourself in and wait for morning, or sleep in a bed.",
                            "Blijf uit het donker! Sluit jezelf in en wacht op de ochtend, of slaap in een bed."),
    T + "thunder.1": ("A thunderstorm! Monsters can spawn even during the day now. Find cover.",
                      "Onweer! Monsters kunnen nu ook overdag verschijnen. Zoek dekking."),
    T + "underground.1": ("You're underground without torches. Bring torches, and never dig straight down: there might be lava below!",
                          "Je bent ondergronds zonder fakkels. Neem fakkels mee en graaf nooit recht naar beneden: er kan lava onder je zitten!"),
    T + "underground.2": ("Careful down here! Dig like a staircase, not straight down, and keep some blocks with you.",
                          "Voorzichtig hier beneden! Graaf als een trap, niet recht omlaag, en hou wat blokken bij je."),

    # ---------------- milestones ----------------
    T + "wood.1": ("First things first: punch a tree! Hold the attack button on a log. Get about 10 to 16 logs.",
                   "Eerst het belangrijkste: hak een boom! Hou de aanvalsknop ingedrukt op een stam. Verzamel zo'n 10 tot 16 blokken hout."),
    T + "wood.2": ("Everything starts with wood. Collect at least 10 logs. Don't leave floating tree tops behind!",
                   "Alles begint met hout. Verzamel minstens 10 stammen. Laat geen zwevende boomtoppen achter!"),
    T + "wood.3": ("See that tree? Wood becomes planks, tools and your first shelter. Gather 10 or more logs.",
                   "Zie je die boom? Hout wordt planken, gereedschap en je eerste schuilplaats. Verzamel 10 of meer stammen."),
    T + "crafting_table.1": ("Open your inventory, turn logs into planks (1 log = 4 planks), then make a crafting table from 4 planks.",
                             "Open je inventaris, maak planken van je hout (1 stam = 4 planken) en maak dan een werkbank van 4 planken."),
    T + "crafting_table.2": ("Time for a crafting table: 4 planks in the 2x2 grid. Place it and you can craft much more.",
                             "Tijd voor een werkbank: 4 planken in het 2x2-raster. Zet hem neer, dan kun je veel meer maken."),
    T + "wooden_pickaxe.1": ("Make sticks (2 planks on top of each other) and a wooden pickaxe: 3 planks on top, 2 sticks below.",
                             "Maak stokken (2 planken op elkaar) en een houten houweel: 3 planken bovenaan, 2 stokken eronder."),
    T + "wooden_pickaxe.2": ("You only need one wooden tool: a pickaxe. With it you can mine stone for much better tools.",
                             "Je hebt maar één houten gereedschap nodig: een houweel. Daarmee hak je steen voor veel beter gereedschap."),
    T + "stone_tools.1": ("Mine 15 to 20 stone with your pickaxe. Then make a stone pickaxe and a stone sword or axe.",
                          "Hak 15 tot 20 steen met je houweel. Maak dan een stenen houweel en een stenen zwaard of bijl."),
    T + "stone_tools.2": ("Stone tools are much better than wood. Find some exposed stone, no need to dig deep!",
                          "Stenen gereedschap is veel beter dan hout. Zoek steen aan de oppervlakte, je hoeft niet diep te graven!"),
    T + "stone_tools.3": ("Look, stone over there! Get some cobblestone and make a stone pickaxe and a weapon.",
                          "Kijk, daar ligt steen! Haal wat keien en maak een stenen houweel en een wapen."),
    T + "furnace.1": ("Make a furnace: 8 cobblestone in a ring on the crafting table. You'll need it to cook food and smelt ore.",
                      "Maak een oven: 8 keien in een ring op de werkbank. Die heb je nodig om eten te bakken en erts te smelten."),
    T + "furnace.2": ("A furnace turns raw meat into cooked meat, which fills you up much better. 8 cobblestone is all it takes.",
                      "Een oven maakt van rauw vlees gebakken vlees, dat vult veel beter. Je hebt alleen 8 keien nodig."),
    T + "food.1": ("Collect food before it gets dark: cows, pigs, sheep and chickens. Aim for at least 5 pieces.",
                   "Verzamel eten voor het donker wordt: koeien, varkens, schapen en kippen. Probeer minstens 5 stuks."),
    T + "food.2": ("Your hunger bar keeps dropping. Hunt a few animals and cook the meat in your furnace.",
                   "Je hongerbalk loopt steeds verder terug. Jaag op een paar dieren en bak het vlees in je oven."),
    T + "food.3": ("Food tip: cooked meat is much better than raw. Gather a stock of at least 5 pieces.",
                   "Eettip: gebakken vlees is veel beter dan rauw. Leg een voorraad aan van minstens 5 stuks."),
    T + "bed.1": ("Make a bed: 3 wool and 3 planks. Each sheep you defeat drops 1 wool; with shears you get 1 to 3.",
                  "Maak een bed: 3 wol en 3 planken. Elk verslagen schaap geeft 1 wol; met een schaar krijg je er 1 tot 3."),
    T + "bed.2": ("A bed lets you skip the night AND it becomes your respawn point. Look for 3 sheep!",
                  "Met een bed sla je de nacht over EN het wordt je respawnpunt. Zoek 3 schapen!"),
    T + "torches.1": ("Look for black-speckled coal in the rock. 1 coal + 1 stick = 4 torches. Make at least 8.",
                      "Zoek kolen (stenen met zwarte stippen). 1 kool + 1 stok = 4 fakkels. Maak er minstens 8."),
    T + "torches.2": ("No coal? Smelt a log in your furnace to get charcoal; it works the same for torches.",
                      "Geen kolen? Smelt een stam in je oven tot houtskool, daar kun je ook fakkels van maken."),
    T + "shelter.1": ("Prepare a shelter before night: a small hut or a room dug into a hill. Close every gap and add torches.",
                      "Maak een schuilplaats voor de nacht: een hutje of een kamer in een heuvel. Dicht elk gat en zet er fakkels in."),
    T + "shelter.2": ("Digging into a hill is the quickest shelter. Use a door so you can see when it's morning.",
                      "In een heuvel graven is de snelste schuilplaats. Gebruik een deur, dan zie je wanneer het ochtend is."),
    T + "shelter.3": ("Monsters only spawn in complete darkness. A lit room with no open gaps is safe.",
                      "Monsters verschijnen alleen in volledig donker. Een verlichte ruimte zonder open gaten is veilig."),
    T + "chest.1": ("Make a chest (8 planks in a ring) and store your valuables at home. Also note your coordinates with F3!",
                    "Maak een kist (8 planken in een ring) en bewaar je waardevolle spullen thuis. Noteer ook je coördinaten met F3!"),
    T + "iron.1": ("Time for iron! Look for stone with beige-orange specks in cave entrances or cliffs, and smelt it in the furnace.",
                   "Tijd voor ijzer! Zoek steen met beige-oranje stipjes bij grotingangen of rotswanden en smelt het in je oven."),
    T + "iron.2": ("Iron is the big step forward. Bring torches and blocks, and mine the iron you see in cave walls.",
                   "IJzer is de grote stap vooruit. Neem fakkels en blokken mee en hak het ijzer dat je in grotwanden ziet."),
    T + "iron.3": ("Get at least 3 iron ore. Smelt the raw iron into ingots in your furnace.",
                   "Haal minstens 3 ijzererts. Smelt het ruwe ijzer in je oven tot staven."),
    T + "shield.1": ("Make a shield: 6 planks and 1 iron ingot. It's the single best way to survive fights. Hold it in your off-hand.",
                     "Maak een schild: 6 planken en 1 ijzerstaaf. Het is de allerbeste overlevingsupgrade. Hou het in je andere hand."),
    T + "shield.2": ("With a shield you can block arrows and even creeper explosions. Only 1 iron needed!",
                     "Met een schild blokkeer je pijlen en zelfs creeper-explosies. Maar 1 ijzer nodig!"),
    T + "iron_pickaxe.1": ("Make an iron pickaxe (3 iron + 2 sticks). Now you can mine gold, redstone and diamonds.",
                           "Maak een ijzeren houweel (3 ijzer + 2 stokken). Nu kun je goud, redstone en diamant hakken."),
    T + "iron_armor.1": ("Make iron armor, the chestplate first (8 iron). It protects you a lot better.",
                         "Maak een ijzeren harnas, eerst het borstpantser (8 ijzer). Het beschermt je veel beter."),
    T + "iron_armor.2": ("Armor saves lives. Collect more iron and make a chestplate, helmet, leggings and boots.",
                         "Een harnas redt levens. Verzamel meer ijzer en maak een borstpantser, helm, beenbeschermers en laarzen."),
    T + "water_bucket.1": ("Make a bucket (3 iron) and fill it with water. It puts out fire, tames lava and breaks your fall.",
                           "Maak een emmer (3 ijzer) en vul hem met water. Hij blust vuur, maakt lava onschadelijk en breekt je val."),

    # ---------------- chapter 2 tips ----------------
    T + "farm.1": ("Start a farm: make a hoe, till dirt next to water and plant 6 seeds. Seeds drop from tall grass.",
                   "Begin een akker: maak een schoffel, bewerk aarde naast water en plant 6 zaadjes. Zaad valt uit hoog gras."),
    T + "farm.2": ("Food that grows back by itself: plant wheat seeds, carrots or potatoes on tilled dirt near water.",
                   "Eten dat vanzelf aangroeit: plant tarwezaad, wortels of aardappels op bewerkte aarde bij water."),
    T + "bread.1": ("Harvest ripe wheat and bake bread: 3 wheat in a row on the crafting table. No furnace needed.",
                    "Oogst rijpe tarwe en maak brood: 3 tarwe op een rij in de werkbank. Geen oven nodig."),
    T + "breed.1": ("Breed animals: give two cows or sheep wheat, two pigs carrots, two chickens seeds. Fence them in first.",
                    "Fok dieren: geef twee koeien of schapen tarwe, twee varkens wortels, twee kippen zaad. Zet ze eerst achter een hek."),
    T + "breed.2": ("A small animal pen means meat, leather and wool without searching. Feed two animals the same food.",
                    "Een kleine dierenwei geeft vlees, leer en wol zonder zoeken. Voer twee dieren hetzelfde voer."),
    T + "full_iron_armor.1": ("Complete your iron armor: helmet (5), chestplate (8), leggings (7) and boots (4 iron).",
                              "Maak je ijzeren harnas compleet: helm (5), borstplaat (8), beenstukken (7) en laarzen (4 ijzer)."),
    T + "bow.1": ("Make a bow (3 sticks, 3 string from spiders) and 16 arrows (flint from gravel, stick, feather).",
                  "Maak een boog (3 stokken, 3 touw van spinnen) en 16 pijlen (vuursteen uit grind, stok, veer)."),
    T + "bow.2": ("With a bow you can deal with skeletons and creepers from a safe distance.",
                  "Met een boog pak je skeletten en creepers aan van een veilige afstand."),
    T + "diamonds.1": ("Time for diamonds! Mine deep down with an iron pickaxe. Look at the environment card for the best height.",
                       "Tijd voor diamanten! Hak diep onder de grond met een ijzeren houweel. Kijk op de omgevingskaart voor de beste hoogte."),
    T + "diamonds.2": ("Diamonds are deep and often near lava. Bring a water bucket and never dig straight down.",
                       "Diamanten zitten diep en vaak bij lava. Neem een emmer water mee en graaf nooit recht omlaag."),
    T + "diamond_pickaxe.1": ("Make a diamond pickaxe (3 diamonds, 2 sticks). It's the only pickaxe that can mine obsidian.",
                              "Maak een diamanten houweel (3 diamanten, 2 stokken). Alleen daarmee kun je obsidiaan hakken."),
    T + "obsidian.1": ("Collect 10 obsidian: pour water on still lava and mine it with your diamond pickaxe. Enough for a Nether portal.",
                       "Verzamel 10 obsidiaan: giet water op stilstaande lava en hak het met je diamanten houweel. Genoeg voor een Netherportaal."),
    T + "obsidian.2": ("Careful around lava: stand to the side, and keep a block between you and the lava.",
                       "Voorzichtig bij lava: sta ernaast en hou een blok tussen jou en de lava."),
    T + "flint_and_steel.1": ("Make a flint and steel: 1 iron ingot and 1 flint. It lights your Nether portal later.",
                              "Maak een vuursteen en staal: 1 ijzerstaaf en 1 vuursteen. Daarmee steek je later je portaal aan."),
    T + "enchanting_table.1": ("Make an enchanting table: a book (3 paper + leather), 2 diamonds and 4 obsidian.",
                               "Maak een betoveringstafel: een boek (3 papier + leer), 2 diamanten en 4 obsidiaan."),

    # ---------------- praise ----------------
    P + "praise.wood": ("Nice, enough wood to get started!", "Mooi, genoeg hout om mee te beginnen!"),
    P + "praise.crafting_table": ("A crafting table! Now the real crafting begins.", "Een werkbank! Nu begint het echte werk."),
    P + "praise.wooden_pickaxe": ("Your first pickaxe. Well done!", "Je eerste houweel. Goed gedaan!"),
    P + "praise.stone_tools": ("Stone tools: you're getting stronger!", "Stenen gereedschap: je wordt sterker!"),
    P + "praise.furnace": ("A furnace! Time to cook.", "Een oven! Tijd om te koken."),
    P + "praise.food": ("Food in stock. You won't go hungry tonight.", "Eten op voorraad. Je hoeft vannacht geen honger te lijden."),
    P + "praise.bed": ("A bed! Sleep in it to skip the night and set your respawn point.", "Een bed! Slaap erin om de nacht over te slaan en je respawnpunt te zetten."),
    P + "praise.torches": ("Torches! Light keeps the monsters away.", "Fakkels! Licht houdt monsters weg."),
    P + "praise.shelter": ("You survived the first night. Great job!", "Je hebt de eerste nacht overleefd. Super!"),
    P + "praise.chest": ("A chest for your treasures. Smart!", "Een kist voor je schatten. Slim!"),
    P + "praise.iron": ("Iron! This changes everything.", "IJzer! Dit verandert alles."),
    P + "praise.shield": ("A shield! Keep it in your off-hand.", "Een schild! Hou het in je andere hand."),
    P + "praise.iron_pickaxe": ("An iron pickaxe. Diamonds are within reach!", "Een ijzeren houweel. Diamanten zijn binnen bereik!"),
    P + "praise.iron_armor": ("Iron armor! You're much tougher now.", "Een ijzeren harnas! Je bent nu veel sterker."),
    P + "praise.water_bucket": ("A water bucket: every explorer's best friend.", "Een emmer water: de beste vriend van elke ontdekkingsreiziger."),
    P + "praise.farm": ("Your first farm! Food that grows back.", "Je eerste akker! Eten dat vanzelf aangroeit."),
    P + "praise.bread": ("Fresh bread, well done.", "Vers brood, goed gedaan."),
    P + "praise.breed": ("Baby animals! Your own herd is growing.", "Jonge dieren! Je eigen kudde groeit."),
    P + "praise.full_iron_armor": ("Full armor: you're a tank now.", "Volledig harnas: je bent nu een tank."),
    P + "praise.bow": ("Bow and arrows ready. Skeletons, beware!", "Boog en pijlen klaar. Skeletten, pas op!"),
    P + "praise.diamonds": ("Diamonds! The best find in the game.", "Diamanten! De mooiste vondst in het spel."),
    P + "praise.diamond_pickaxe": ("A diamond pickaxe: nothing can stop you now.", "Een diamanten houweel: niets houdt je nog tegen."),
    P + "praise.obsidian": ("Enough obsidian for a Nether portal.", "Genoeg obsidiaan voor een Netherportaal."),
    P + "praise.flint_and_steel": ("Flint and steel ready.", "Vuursteen en staal klaar."),
    P + "praise.enchanting_table": ("An enchanting table! Time to make your gear stronger.", "Een betoveringstafel! Tijd om je spullen sterker te maken."),

    # ---------------- HUD task card ----------------
    P + "hud.title": ("Guide · Day %s/%s", "Gids · Dag %s/%s"),
    P + "hud.title_ch2": ("Guide · Ch. 2 · Day %s/%s", "Gids · H2 · Dag %s/%s"),
    P + "hud.all_done": ("All goals reached!", "Alle doelen gehaald!"),

    # ---------------- guide skins (/guideskin) ----------------
    P + "skin.list_header": ("Guide skins (%s in the skins folder). Click one to use it:", "Gidsskins (%s in de skinsmap). Klik er een aan om hem te gebruiken:"),
    P + "skin.empty": ("No skins yet: put 64x64 skin PNGs in the folder and use /guideskin reload.",
                       "Nog geen skins: zet 64x64 skin-PNG's in de map en gebruik /guideskin reload."),
    P + "skin.click_to_use": ("Click to use this skin", "Klik om deze skin te gebruiken"),
    P + "skin.not_found": ("Skin '%s' not found. Use /guideskin list (or /guideskin reload after adding files).",
                           "Skin '%s' niet gevonden. Gebruik /guideskin list (of /guideskin reload na het toevoegen van bestanden)."),
    P + "skin.set": ("The guide now uses the skin '%s'. Only you see this.", "De gids gebruikt nu de skin '%s'. Alleen jij ziet dit."),
    P + "skin.reloaded": ("Skins folder read again: %s skin(s) found.", "Skinsmap opnieuw gelezen: %s skin(s) gevonden."),
    P + "skin.bad_file": ("Skipped %s: a skin must be a 64x64 PNG.", "Overgeslagen: %s. Een skin moet een PNG van 64x64 zijn."),
    P + "skin.fallback": ("Guide skin '%s' is missing, so the guide uses its own skin.", "Gidsskin '%s' ontbreekt, dus de gids gebruikt zijn eigen skin."),
    P + "skin.folder": ("Skins folder: %s", "Skinsmap: %s"),
    P + "skin.open_folder": ("Click to open the folder", "Klik om de map te openen"),

    # ---------------- light check (L) ----------------
    P + "light.on": ("Light check on: red dots = monsters can spawn there. Place torches until they're gone.",
                     "Verlichtingscheck aan: rode stippen = hier kunnen monsters spawnen. Zet fakkels tot ze weg zijn."),
    P + "light.off": ("Light check off", "Verlichtingscheck uit"),
    P + "light.no_effect": ("Light check: in this dimension light doesn't stop monsters from spawning.",
                            "Verlichtingscheck: in deze dimensie houdt licht monsters niet tegen."),
    P + "light.all_lit": ("Light check: everything nearby is lit. No monsters can spawn here.",
                          "Verlichtingscheck: alles in de buurt is verlicht. Hier spawnen geen monsters."),

    # ---------------- recipe card ----------------
    P + "recipe.title": ("Recipe: %s", "Recept: %s"),
    P + "recipe.needs_table": ("Needs a crafting table (3x3)", "Werkbank nodig (3x3)"),

    # ---------------- crosshair info: ores ----------------
    P + "cross.needs": ("%s: needs a %s", "%s: %s nodig"),
    P + "cross.ok": ("%s: your tool can mine this", "%s: je gereedschap kan dit hakken"),
    P + "tool.wooden_pickaxe": ("wooden pickaxe (or better)", "houten houweel (of beter)"),
    P + "tool.stone_pickaxe": ("stone pickaxe (or better)", "stenen houweel (of beter)"),
    P + "tool.iron_pickaxe": ("iron pickaxe (or better)", "ijzeren houweel (of beter)"),
    P + "tool.diamond_pickaxe": ("diamond pickaxe (or better)", "diamanten houweel (of beter)"),

    # ---------------- crosshair info: mobs ----------------
    P + "mob.creeper": ("explodes about 1.5 s after it starts hissing. Back off, hit it and back off again.",
                        "ontploft zo'n 1,5 s nadat hij gaat sissen. Afstand houden, slaan en weer terug."),
    P + "mob.zombie": ("slow, burns in sunlight. A good first fight with a sword.",
                       "langzaam, verbrandt in zonlicht. Een goed eerste gevecht met een zwaard."),
    P + "mob.zombie_villager": ("fights like a zombie. Later you can cure it: splash potion of weakness, then a golden apple.",
                                "vecht als een zombie. Later te genezen: werpdrankje van zwakte, dan een gouden appel."),
    P + "mob.husk": ("desert zombie that does NOT burn in the sun. Its hit makes you hungry.",
                     "woestijnzombie die NIET verbrandt in de zon. Zijn klap maakt je hongerig."),
    P + "mob.drowned": ("lives in water, some throw tridents. Stay out of deep water at night.",
                        "leeft in water, sommige gooien drietanden. 's Nachts uit diep water blijven."),
    P + "mob.skeleton": ("shoots arrows, burns in sunlight. Use cover or a shield and close in fast.",
                         "schiet pijlen, verbrandt in zonlicht. Zoek dekking of gebruik een schild en ga er snel op af."),
    P + "mob.stray": ("snow skeleton: its arrows slow you down.",
                      "sneeuwskelet: zijn pijlen maken je traag."),
    P + "mob.bogged": ("swamp skeleton: its arrows poison you.",
                       "moerasskelet: zijn pijlen vergiftigen je."),
    P + "mob.parched": ("desert skeleton: its arrows weaken you, so you deal less damage.",
                        "woestijnskelet: zijn pijlen verzwakken je, je doet dan minder schade."),
    P + "mob.spider": ("climbs walls. In daylight it leaves you alone unless you hit it. Drops string.",
                       "klimt tegen muren. Overdag laat hij je met rust tenzij je hem slaat. Laat touw vallen."),
    P + "mob.cave_spider": ("small and poisonous, found in mineshafts. Block its spawner with torches.",
                            "klein en giftig, zit in mijnschachten. Zet fakkels rond de spawner."),
    P + "mob.enderman": ("don't look at its eyes. Hide under a 2-block-high roof or stand in water.",
                         "kijk hem niet in de ogen. Schuil onder een dak van 2 blokken hoog of ga in water staan."),
    P + "mob.witch": ("throws harmful potions and heals itself. Avoid it early on.",
                      "gooit schadelijke drankjes en geneest zichzelf. Vermijd haar in het begin."),
    P + "mob.slime": ("splits into smaller slimes when hit. The smallest ones are harmless.",
                      "splitst in kleinere slijmen als je hem raakt. De kleinste zijn ongevaarlijk."),
    P + "mob.phantom": ("comes when you haven't slept for 3 nights. Sleep in a bed to stop them.",
                        "komt als je 3 nachten niet hebt geslapen. Slaap in een bed om ze te stoppen."),
    P + "mob.pillager": ("shoots a crossbow and travels in groups. Avoid them early on.",
                         "schiet met een kruisboog en trekt in groepen rond. Vermijd ze in het begin."),
    P + "mob.silverfish": ("hides in stone. When hit, it wakes up other silverfish nearby. Finish it fast.",
                           "verstopt zich in steen. Geraakt maakt hij andere zilvervisjes wakker. Maak het snel af."),
    P + "mob.creaking": ("can't be hurt and only moves when you look away. Break its creaking heart.",
                         "kan geen schade krijgen en beweegt alleen als je wegkijkt. Breek zijn krakende hart."),
    P + "mob.sheep": ("wool for a bed. Shears give 1-3 wool, killing gives 1.",
                      "wol voor een bed. Een schaar geeft 1-3 wol, doden geeft 1."),
    P + "mob.cow": ("beef and leather. Milk (bucket) removes bad effects.",
                    "biefstuk en leer. Melk (emmer) haalt slechte effecten weg."),
    P + "mob.pig": ("drops porkchops: cook them in a furnace for good food.",
                    "laat karbonades vallen: bak ze in een oven voor goed eten."),
    P + "mob.chicken": ("feathers for arrows, eggs, and chicken meat (cook it first).",
                        "veren voor pijlen, eieren en kippenvlees (eerst bakken)."),
    P + "mob.wolf": ("tame it with bones and it will fight for you.",
                     "tem hem met botten en hij vecht voor je."),
    P + "hud.mode.compact": ("Task list: compact", "Takenlijst: compact"),
    P + "hud.mode.full": ("Task list: full", "Takenlijst: volledig"),
    P + "hud.mode.hidden": ("Task list: hidden", "Takenlijst: verborgen"),
    "key.survivalassistant.toggle_hud": ("Guide task list", "Gids-takenlijst"),
    "key.category.survivalassistant.guide": ("Survival Assistant", "Survival Assistant"),

    P + "task.wood": ("Chop wood", "Hak hout"),
    P + "task.crafting_table": ("Make a crafting table", "Werkbank maken"),
    P + "task.wooden_pickaxe": ("Wooden pickaxe", "Houten houweel"),
    P + "task.stone_tools": ("Stone pickaxe + sword", "Stenen houweel + zwaard"),
    P + "task.furnace": ("Build a furnace", "Oven bouwen"),
    P + "task.food": ("Gather food", "Eten verzamelen"),
    P + "task.bed": ("Make a bed (wool)", "Bed maken (wol)"),
    P + "task.torches": ("Make torches", "Fakkels maken"),
    P + "task.shelter": ("Shelter before night", "Schuilplaats voor de nacht"),
    P + "task.chest": ("Chest + note coordinates", "Kist + coördinaten noteren"),
    P + "task.iron": ("Find and smelt iron", "IJzer zoeken en smelten"),
    P + "task.shield": ("Make a shield", "Schild maken"),
    P + "task.iron_pickaxe": ("Iron pickaxe", "IJzeren houweel"),
    P + "task.iron_armor": ("Iron armor", "IJzeren harnas"),
    P + "task.water_bucket": ("Bucket of water", "Emmer water"),
    P + "task.farm": ("Start a farm", "Begin een akker"),
    P + "task.bread": ("Bake bread", "Bak brood"),
    P + "task.breed": ("Breed animals", "Fok dieren"),
    P + "task.full_iron_armor": ("Full iron armor", "Volledig ijzeren harnas"),
    P + "task.bow": ("Bow and arrows", "Boog en pijlen"),
    P + "task.diamonds": ("Find diamonds", "Vind diamanten"),
    P + "task.diamond_pickaxe": ("Diamond pickaxe", "Diamanten houweel"),
    P + "task.obsidian": ("Collect obsidian", "Verzamel obsidiaan"),
    P + "task.flint_and_steel": ("Flint and steel", "Vuursteen en staal"),
    P + "task.enchanting_table": ("Enchanting table", "Betoveringstafel"),

    P + "alert.creeper": ("Creeper! Run!", "Creeper! Ren weg!"),
    P + "alert.danger": ("Low health, retreat!", "Weinig levens, terug!"),
    P + "alert.hungry_eat": ("Hungry: eat now!", "Honger: eet nu!"),
    P + "alert.hungry_hunt": ("Hungry: find food!", "Honger: zoek eten!"),
    P + "alert.dusk": ("Sunset: find cover!", "Zonsondergang: dekking!"),
    P + "alert.night_outside": ("Night: get inside!", "Nacht: naar binnen!"),
    P + "alert.thunder": ("Thunderstorm: cover!", "Onweer: zoek dekking!"),
    P + "alert.underground": ("No torches down here!", "Geen fakkels hier!"),

    # ---------------- 0.5: safety and navigation ----------------
    T + "dig_down.1": ("Stop! You're digging straight down. You could fall into lava or a deep cave. Dig like a staircase instead.",
                       "Stop! Je graaft recht naar beneden. Je kunt in lava of een diepe grot vallen. Graaf liever als een trap."),
    T + "dig_down.2": ("Never dig the block under your own feet. Mine one block ahead and one down, step by step.",
                       "Graaf nooit het blok onder je eigen voeten weg. Hak een blok voor je en een omlaag, stap voor stap."),
    T + "lava_near.1": ("Careful, there's lava very close! Don't mine towards it; put a block in between.",
                        "Voorzichtig, er is lava heel dichtbij! Hak er niet naartoe; zet er een blok tussen."),
    T + "lava_near.2": ("Lava nearby! Move slowly (sneak) and keep a water bucket or blocks ready.",
                        "Lava in de buurt! Beweeg rustig (sluipen) en hou een emmer water of blokken klaar."),
    T + "phantoms.1": ("You haven't slept for two nights. After the third night phantoms come for you from the sky. Sleep in a bed!",
                       "Je hebt twee nachten niet geslapen. Na de derde nacht komen phantoms je uit de lucht aanvallen. Slaap in een bed!"),
    T + "phantoms.2": ("Tired? Sleep tonight: without sleep, flying phantoms appear after three nights.",
                       "Moe? Slaap vannacht: zonder slaap verschijnen er na drie nachten vliegende phantoms."),
    T + "falling_block.1": ("Watch out: there's sand or gravel right above your head. It falls when you mine below it.",
                            "Let op: er zit zand of grind recht boven je hoofd. Dat valt naar beneden als je eronder hakt."),
    P + "alert.dig_down": ("Don't dig straight down!", "Niet recht omlaag graven!"),
    P + "alert.lava_near": ("Lava nearby!", "Lava dichtbij!"),
    P + "alert.phantoms": ("Sleep tonight!", "Slaap vannacht!"),
    P + "alert.falling_block": ("Gravel above you!", "Grind boven je!"),
    P + "nav.death": ("Your items: %s blocks, max %s left", "Je spullen: %s blokken, nog max %s"),
    P + "nav.death_other": ("Your items: in the %s, max %s left", "Je spullen: in de %s, nog max %s"),
    P + "nav.home": ("Home: %s blocks", "Naar huis: %s blokken"),
    P + "nav.dim.overworld": ("Overworld", "Overworld"),
    P + "nav.dim.the_nether": ("Nether", "Nether"),
    P + "nav.dim.the_end": ("End", "End"),

    # ---------------- guide character on/off ----------------
    P + "cmd.chat_on": ("Small talk on. The guide needs a local language model for this (llama-server at %s); without one it stays quiet.",
                        "Kletsen staat aan. De gids heeft hiervoor een lokaal taalmodel nodig (llama-server op %s); zonder blijft hij stil."),
    P + "cmd.chat_off": ("Small talk off. Tips and warnings keep working as usual.",
                         "Kletsen staat uit. Tips en waarschuwingen werken gewoon door."),
    P + "cmd.character_on": ("The guide character is back. Use /guide character off to hide it again.",
                             "Het gidskarakter is terug. Met /guide character off verberg je het weer."),
    P + "cmd.character_off": ("Guide character off. Your task list stays, and you'll only get urgent warnings in chat.",
                              "Gidskarakter uit. Je takenlijst blijft, en in de chat krijg je alleen nog dringende waarschuwingen."),
    P + "cmd.npc_off_recall": ("The guide character is off. Turn it on with /guide character on.",
                               "Het gidskarakter staat uit. Zet het aan met /guide character on."),
    P + "welcome_no_npc": ("Your task list is in the corner of the screen (J switches the view, K shows or hides the environment card). I'll only speak up when there's danger.",
                           "Je takenlijst staat in de hoek van je scherm (J wisselt de weergave, K toont of verbergt de omgevingskaart). Ik meld me alleen bij gevaar."),

    # ---------------- environment card ----------------
    P + "hud.env_on": ("Environment card: on", "Omgevingskaart: aan"),
    P + "hud.env_off": ("Environment card: off", "Omgevingskaart: uit"),
    "key.survivalassistant.toggle_env": ("Environment card", "Omgevingskaart"),
    "key.survivalassistant.light_check": ("Light check (dark spots)", "Verlichtingscheck (donkere plekken)"),
    P + "env.here": ("here:", "hier:"),
    P + "env.no_ores": ("no ores at this height", "geen ertsen op deze hoogte"),
    P + "env.pointer.up": ("%s: go up, most around Y %s", "%s: ga omhoog, het meest rond Y %s"),
    P + "env.pointer.down": ("%s: go down, most around Y %s", "%s: ga omlaag, het meest rond Y %s"),
    P + "env.pointer.here": ("%s: you're at a good height", "%s: je zit op een goede hoogte"),
    P + "env.pointer.mountain": ("%s: lots of it at this height (mountains)", "%s: hier veel van op deze hoogte (bergen)"),
    P + "env.hazard.deep_dark": ("Deep dark: sneak, the warden!", "Deep dark: sluip, de warden!"),
    P + "env.hazard.lava": ("Below Y -54: lava lakes", "Onder Y -54: lavameren"),
    P + "env.hazard.deepslate": ("Deepslate: slower to mine", "Deepslate: hakken gaat trager"),

    P + "ore.diamond": ("Diamond", "Diamant"),
    P + "ore.emerald": ("Emerald", "Smaragd"),
    P + "ore.gold": ("Gold", "Goud"),
    P + "ore.redstone": ("Redstone", "Redstone"),
    P + "ore.lapis": ("Lapis", "Lapis"),
    P + "ore.iron": ("Iron", "IJzer"),
    P + "ore.copper": ("Copper", "Koper"),
    P + "ore.coal": ("Coal", "Kolen"),

    # one-time depth notices (chat)
    P + "depth.deepslate": ("You've reached deepslate (below Y 0). Diamonds start below Y 16 and are most common around Y -59. Mining is slower here.",
                            "Je bent in deepslate (onder Y 0). Diamanten beginnen onder Y 16 en zitten het meest rond Y -59. Hakken gaat hier trager."),
    P + "depth.lava": ("Careful: below Y -54 caves fill with lava lakes. Keep a water bucket and blocks at hand.",
                       "Pas op: onder Y -54 lopen grotten vol met lavameren. Hou een emmer water en blokken bij de hand."),
    P + "depth.deep_dark": ("This is the deep dark. Sneak, don't trigger the sculk shriekers, or the warden will come!",
                            "Dit is de deep dark. Sluip, en laat de sculk shriekers niet afgaan, anders komt de warden!"),
    P + "depth.high_iron": ("High up in the mountains there's extra iron (and emerald ore) in the stone. Great for your iron gear!",
                            "Hoog in de bergen zit extra veel ijzer (en smaragderts) in de steen. Ideaal voor je ijzeren uitrusting!"),

    # ---------------- biome pros/cons (checked against docs/biomes-and-heights.md) ----------------
    **{P + f"biome.{g}.{k}": v for g, pc in {
        "plains": (("All farm animals and flat land to build on", "Alle boerderijdieren en vlak land om op te bouwen"),
                   ("Few trees and little cover: build a shelter in time", "Weinig bomen en dekking: bouw op tijd een schuilplaats")),
        "forest": (("Lots of wood, apples and all farm animals", "Veel hout, appels en alle boerderijdieren"),
                   ("Under leaves, monsters don't burn by day", "Onder bladeren verbranden monsters overdag niet")),
        "birch_forest": (("Light forest: plenty of wood and farm animals", "Licht bos: veel hout en boerderijdieren"),
                         ("No oaks, so no apples from the leaves", "Geen eiken, dus geen appels uit de bladeren")),
        "dark_forest": (("Huge dark oaks: loads of wood and apples", "Enorme donkere eiken: heel veel hout en appels"),
                        ("So dark that monsters can spawn by day", "Zo donker dat monsters ook overdag kunnen verschijnen")),
        "pale_garden": (("Pale oak wood and resin from creaking hearts", "Bleek eikenhout en hars uit creaking hearts"),
                        ("No animals to eat; creaking at night", "Geen dieren om te eten; 's nachts creakings")),
        "dappled_forest": (("All farm animals and brown mushrooms", "Alle boerderijdieren en bruine paddenstoelen"),
                           ("Ordinary forest dangers: stay inside at night", "Gewone bosgevaren: blijf 's nachts binnen")),
        "taiga": (("Spruce wood, sweet berries and all farm animals", "Sparrenhout, zoete bessen en alle boerderijdieren"),
                  ("Sweet berry bushes hurt when you walk through", "Zoetebessenstruiken doen pijn als je erdoor loopt")),
        "snowy": (("Snow and ice to build with; rabbits to hunt", "Sneeuw en ijs om mee te bouwen; konijnen om op te jagen"),
                  ("Little food, and strays shoot slowing arrows", "Weinig eten, en strays schieten vertragende pijlen")),
        "mountains": (("Lots of iron high up and emerald ore", "Veel ijzer hoog in de steen, en smaragderts"),
                      ("Steep drops, ramming goats, powder snow", "Afgronden, rammende geiten, poedersneeuw")),
        "meadow": (("Sheep for wool, flowers, and emerald ore", "Schapen voor wol, bloemen, en smaragderts"),
                   ("High up and open: mind the edges at night", "Hoog en open: let 's nachts op de randen")),
        "desert": (("Sand for glass, cactus, temples and villages", "Zand voor glas, cactussen, tempels en dorpen"),
                   ("Only rabbits and camels; husks make you hungry", "Alleen konijnen en kamelen; husks geven je honger")),
        "badlands": (("Extra gold ore up high (Y 32-256)", "Extra goud hoog in de grond (Y 32-256)"),
                     ("Very little wood (except wooded badlands)", "Heel weinig hout (behalve in wooded badlands)")),
        "savanna": (("Acacia wood, farm animals, horses and villages", "Acaciahout, boerderijdieren, paarden en dorpen"),
                    ("Open land with little cover at night", "Open land met weinig dekking in de nacht")),
        "jungle": (("Melons, cocoa beans, bamboo and lots of wood", "Meloenen, cacaobonen, bamboe en veel hout"),
                   ("Dense and confusing: you get lost easily", "Dicht en onoverzichtelijk: je verdwaalt snel")),
        "swamp": (("Clay, lily pads and slimes (slimeballs)", "Klei, waterlelies en slijmen (slijmballen)"),
                  ("Witches, and bogged that shoot poison arrows", "Heksen, en bogged die gifpijlen schieten")),
        "shore": (("Sugar cane for paper and sand for glass", "Suikerriet voor papier en zand voor glas"),
                  ("Drowned can come out of rivers at night", "'s Nachts kunnen drowned uit rivieren komen")),
        "ocean": (("Shipwrecks and ruins with loot; fish to eat", "Scheepswrakken en ruïnes met buit; vis om te eten"),
                  ("Drowned with tridents; watch your air bubbles", "Drowned met drietanden; let op je luchtbelletjes")),
        "mushroom": (("No monsters spawn here; mooshrooms give stew", "Hier spawnen geen monsters; mooshrooms geven soep"),
                     ("No trees: bring your own wood", "Geen bomen: neem zelf hout mee")),
        "lush_caves": (("Glow berries to eat, axolotls and clay", "Gloeibessen om te eten, axolotls en klei"),
                       ("Still a cave: monsters spawn in the dark", "Nog steeds een grot: monsters in het donker")),
        "dripstone": (("Large copper ore veins", "Grote aders kopererts"),
                      ("Falling dripstone hurts; drowned in pools", "Vallend druipsteen doet pijn; drowned in poelen")),
        "deep_dark": (("Ancient cities with great loot; no normal mob spawns", "Oude steden met goede buit; geen gewone monsters"),
                      ("The warden: sneak and avoid sculk shriekers", "De warden: sluip en blijf weg van sculk shriekers")),
        "sulfur_caves": (("Sulfur pools, springs and spikes to discover", "Zwavelpoelen, -bronnen en -pieken om te ontdekken"),
                         ("Sulfur cubes spawn here, plus creepers and skeletons", "Hier spawnen sulfur cubes, en ook creepers en skeletten")),
        "nether": (("Quartz, gold and Nether materials", "Kwarts, goud en Nether-materialen"),
                   ("No water, lots of lava: not for your first days", "Geen water, veel lava: niet voor je eerste dagen")),
        "end": (("The end of the game: the dragon and End cities", "Het einde van het spel: de draak en End-steden"),
                ("Endermen everywhere; only go with good gear", "Overal endermen; ga alleen met goede uitrusting")),
        "other": (("Look around for wood, food and stone", "Kijk rond naar hout, eten en steen"),
                  ("Unknown area: no specific advice", "Onbekend gebied: geen specifiek advies")),
    }.items() for k, v in (("pro", pc[0]), ("con", pc[1]))},
}

# ---------------------------------------------------------------- checks and output

# Letters, digits, punctuation, spaces and combining marks of any script are fine: Minecraft's font (with its Unifont
# fallback) has them, and it shapes Arabic and orders right-to-left text itself. Symbols (pickaxes, arrows, emoji, ...)
# may be missing from the font and render as boxes or wrong glyphs, so the HUD draws those as icons instead.
ALLOWED_CATEGORIES = ("L", "M", "N", "P", "Z")
EXTRA_ALLOWED = {"+", "<", ">", "=", "%", "·", "$"}  # used in tips, the HUD title and positional %1$s placeholders
PLACEHOLDER = re.compile(r"%(?:\d+\$)?s")


def check_chars(lang, key, text):
    bad = sorted({ch for ch in text
                  if ch not in EXTRA_ALLOWED and not unicodedata.category(ch).startswith(ALLOWED_CATEGORIES)})
    if bad:
        sys.exit(f"[{lang}] unsafe character(s) {bad} in {key!r}: use plain text or an icon in the HUD code instead.")


def load_translations():
    """tools/lang/<code>.json: key -> text, for every language beyond English and Dutch."""
    here = os.path.dirname(os.path.abspath(__file__))
    out = {}
    for path in sorted(glob.glob(os.path.join(here, "lang", "*.json"))):
        code = os.path.splitext(os.path.basename(path))[0]
        with open(path, encoding="utf-8") as f:
            # "~" is short for "survivalassistant." to keep the translation files readable
            out[code] = {k.replace("~", P): v for k, v in json.load(f).items()}
    return out


languages = {"en_us": {k: v[0] for k, v in L.items()}, "nl_nl": {k: v[1] for k, v in L.items()}}
languages.update(load_translations())

english = languages["en_us"]
for code, table in languages.items():
    missing = [k for k in english if k not in table]
    extra = [k for k in table if k not in english]
    if missing or extra:
        sys.exit(f"[{code}] missing keys: {missing[:10]} ({len(missing)}), unknown keys: {extra[:10]} ({len(extra)})")
    for key, text in table.items():
        check_chars(code, key, text)
        if len(PLACEHOLDER.findall(text)) != len(PLACEHOLDER.findall(english[key])):
            sys.exit(f"[{code}] placeholder count differs from English in {key!r}: {text!r}")

# Minecraft falls back to English when the chosen language has no file, so regional variants with the same script
# get a copy (Traditional Chinese zh_tw/zh_hk do not: different characters).
VARIANTS = {
    "es_es": ["es_mx", "es_ar", "es_cl", "es_ec", "es_uy", "es_ve"],
    "fr_fr": ["fr_ca", "fr_ch"],
    "pt_br": ["pt_pt"],
    "de_de": ["de_at", "de_ch"],
    "nl_nl": ["nl_be"],
}

out_dir = sys.argv[1]
written = 0
for code, table in languages.items():
    for target in [code] + VARIANTS.get(code, []):
        with open(f"{out_dir}/{target}.json", "w", encoding="utf-8") as f:
            json.dump({k: table[k] for k in english}, f, ensure_ascii=False, indent="\t")
            f.write("\n")
        written += 1
print(len(L), "keys,", len(languages), "languages,", written, "files")
