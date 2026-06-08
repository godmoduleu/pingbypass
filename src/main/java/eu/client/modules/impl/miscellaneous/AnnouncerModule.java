package eu.client.modules.impl.miscellaneous;

import eu.client.EUClient;
import eu.client.events.SubscribeEvent;
import eu.client.events.impl.*;
import eu.client.modules.Module;
import eu.client.modules.RegisterModule;
import eu.client.settings.impl.BooleanSetting;
import eu.client.settings.impl.ModeSetting;
import eu.client.settings.impl.NumberSetting;
import eu.client.settings.impl.StringSetting;
import eu.client.utils.system.MathUtils;
import eu.client.utils.system.Timer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ConcurrentLinkedQueue;

@RegisterModule(name = "Announcer", description = "Announces your actions in chat.", category = Module.Category.MISCELLANEOUS)
public class AnnouncerModule extends Module {
    public StringSetting watermark = new StringSetting("Watermark", "The client name that will be used in the announcer.", EUClient.MOD_NAME);
    public ModeSetting language = new ModeSetting("Language", "The language that will be used for the announcer.", "English", new String[]{"English", "German", "French", "Japanese", "Finnish", "Russian", "Spanish", "Swedish", "Turkish", "Dutch", "Greek", "Chinese", "Italian", "Norwegian", "Romanian", "Czech", "Portuguese", "Slovenian", "Polish", "Korean", "Lithuanian", "Indonesian", "Hungarian", "Random"});
    public NumberSetting delay = new NumberSetting("Delay", "The delay for the announcer.", 5, 0, 30);
    public BooleanSetting clientside = new BooleanSetting("Clientside", "Sends the messages only on your side.", false);
    public BooleanSetting greenText = new BooleanSetting("GreenText", "Makes your message green.", false);
    public BooleanSetting distance = new BooleanSetting("Distance", "Announces your distance travelled in chat.", true);
    public BooleanSetting blocksMined = new BooleanSetting("BlocksMined", "Announces blocks mined in chat.", true);
    public BooleanSetting blocksPlaced = new BooleanSetting("BlocksPlaced", "Announces blocks placed in chat.", true);
    public BooleanSetting eating = new BooleanSetting("Eating", "Announces when you eat in chat.", true);

    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    private final Timer messageTimer = new Timer();
    private final Timer distanceTimer = new Timer();

    private Vec3d lastPos;
    private int mined;
    private int placed;
    private int eaten;

    @Override
    public void onEnable() {
        queue.clear();

        messageTimer.reset();
        distanceTimer.reset();

        lastPos = null;
        mined = 0;
        placed = 0;
        eaten = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if(getNull()) return;

        if (messageTimer.hasTimeElapsed(delay.getValue().intValue() * 1000) && !queue.isEmpty()) {
            String message = queue.poll();

            if (clientside.getValue()) EUClient.CHAT_MANAGER.message(message);
            else mc.player.networkHandler.sendChatMessage((greenText.getValue() ? "> " : "") + message);

            messageTimer.reset();
        }

        if(lastPos == null) lastPos = new Vec3d(mc.player.lastRenderX, mc.player.lastRenderY, mc.player.lastRenderZ);
        double traveled = Math.abs(lastPos.x - mc.player.lastRenderX) + Math.abs(lastPos.y - mc.player.lastRenderY) + Math.abs(lastPos.z - mc.player.lastRenderZ);

        if(distance.getValue() && traveled > 1 && distanceTimer.hasTimeElapsed(10000) && queue.size() <= 5) {
            queue.add(getDistanceMessage(MathUtils.round(traveled, 1) + ""));
            lastPos = new Vec3d(mc.player.lastRenderX, mc.player.lastRenderY, mc.player.lastRenderZ);
            distanceTimer.reset();
        }
    }

    @SubscribeEvent
    public void onBreakBlock(BreakBlockEvent event) {
        if(getNull()) return;

        mined++;

        if(blocksMined.getValue() && mined >= MathUtils.random(6, 1) && queue.size() <= 5) {
            queue.add(getMineMessage(mined + ""));
            mined = 0;
        }
    }

    @SubscribeEvent
    public void onPacketSend(PacketSendEvent event) {
        if(getNull()) return;

        if(event.getPacket() instanceof PlayerInteractBlockC2SPacket packet && mc.player.getStackInHand(packet.getHand()).getItem() instanceof BlockItem) {
            placed++;

            if(blocksPlaced.getValue() && placed >= MathUtils.random(6, 1) && queue.size() <= 5) {
                queue.add(getPlaceMessage(placed + ""));
                placed = 0;
            }
        }
    }

    @SubscribeEvent
    public void onConsumeItem(ConsumeItemEvent event) {
        if(getNull() || event.getStack().get(DataComponentTypes.FOOD) == null) return;

        eaten++;

        if(eating.getValue() && eaten >= MathUtils.random(6, 1) && queue.size() <= 5) {
            queue.add(getEatMessage(eaten + " " + event.getStack().getName().getString()));
            eaten = 0;
        }
    }

    @SubscribeEvent
    public void onChatInput(ChatInputEvent event) {
        if(getNull()) return;
        messageTimer.reset();
    }

    private String getDistanceMessage(String replacement) {
        String[] messages = new String[]{
                "I just flew " + replacement + " meters thanks to " + watermark.getValue() + "!",
                "Ich bin gerade " + replacement + " Meter weit geflogen, dank " + watermark.getValue() + "!",
                "Je viens de voler " + replacement + " mètres grâce à " + watermark.getValue() + "!",
                watermark.getValue() + "のおかげで" + replacement + "メートル飛んだよ！",
                "Lensin juuri " + replacement + " metriä " + watermark.getValue() + " ansiosta!",
                "Я только что пролетел " + replacement + " метров благодаря " + watermark.getValue() + "!",
                "Acabo de volar " + replacement + " metros gracias a " + watermark.getValue() + "!",
                "Jag flög just " + replacement + " meter tack vare " + watermark.getValue() + "!",
                watermark.getValue() + " sayesinde " + replacement + " metre uçtum!",
                "Ik heb net " + replacement + " meter gevlogen dankzij " + watermark.getValue() + "!",
                "Μόλις πέταξα " + replacement + " μέτρα χάρη στην " + watermark.getValue() + "!",
                "我刚刚飞了" + replacement + "米，多亏了" + watermark.getValue() + "!",
                "Ho appena volato per " + replacement + " metri grazie ad " + watermark.getValue() + "!",
                "Jeg fløy nettopp " + replacement + " meter takket være " + watermark.getValue() + "!",
                "Tocmai am zburat " + replacement + " de metri datorită lui " + watermark.getValue() + "!",
                "Díky " + watermark.getValue() + " jsem právě uletěl " + replacement + " metrů!",
                "Acabei de voar " + replacement + " metros graças ao " + watermark.getValue() + "!",
                "Z " + watermark.getValue() + " sem pravkar preletel " + replacement + " metrov!",
                "Właśnie przeleciałem " + replacement + " metrów dzięki " + watermark.getValue() + "!",
                watermark.getValue() + " 덕분에 방금 " + replacement + "를 비행했습니다!",
                watermark.getValue() + " dėka ką tik nuskridau " + replacement + " metrus!",
                "Saya baru saja terbang sejauh " + replacement + " meter berkat " + watermark.getValue() + "!",
                replacement + " métert repültem az " + watermark.getValue() + " köszönhetően!"
        };

        return messages[getLanguageIndex()];
    }

    private String getMineMessage(String replacement) {
        String[] messages = new String[]{
                "I just mined " + replacement + " blocks thanks to " + watermark.getValue() + "!",
                "Ich habe gerade " + replacement + " Blöcke abgebaut, dank " + watermark.getValue() + "!",
                "Je viens d'extraire " + replacement + " blocs grâce à " + watermark.getValue() + "!",
                watermark.getValue() + "のおかげで" + replacement + "ブロック採掘したところです！",
                "Louhin juuri " + replacement + " lohkoa " + watermark.getValue() + " ansiosta!",
                "Я только что добыл " + replacement + " блоков благодаря " + watermark.getValue() + "!",
                "¡Acabo de minar " + replacement + " bloques gracias a " + watermark.getValue() + "!",
                "Jag har precis tagit fram " + replacement + " block tack vare " + watermark.getValue() + "!",
                watermark.getValue() + " sayesinde az önce " + replacement + " blok kazdım!",
                "Ik heb net " + replacement + " blokken gedolven dankzij " + watermark.getValue() + "!",
                "Μόλις εξόρυξα " + replacement + " μπλοκ χάρη στην " + watermark.getValue() + "!",
                "我刚刚开采了" + replacement + "个区块，感谢" + watermark.getValue() + "!",
                "Ho appena estratto " + replacement + " blocchi grazie ad " + watermark.getValue() + "!",
                "Jeg har nettopp utvunnet " + replacement + " blokker takket være " + watermark.getValue() + "!",
                "Tocmai am minat " + replacement + " de blocuri datorită lui " + watermark.getValue() + "!",
                "Právě jsem vytěžil " + replacement + " bloků díky " + watermark.getValue() + "!",
                "Acabei de extrair " + replacement + " blocos graças ao " + watermark.getValue() + "!",
                "Zahvaljujoč " + watermark.getValue() + " sem pravkar izkopal " + replacement + " blokov!",
                "Właśnie wydobyłem " + replacement + " bloków dzięki " + watermark.getValue() + "!",
                "방금 " + watermark.getValue() + " 덕분에 " + replacement + " 블록을 채굴했습니다!",
                watermark.getValue() + " dėka ką tik iškasiau " + replacement + " blokus!",
                "Saya baru saja menambang " + replacement + " blok berkat " + watermark.getValue() + "!",
                "Most bányásztam " + replacement + " blokkot az " + watermark.getValue() + " köszönhetően!"
        };

        return messages[getLanguageIndex()];
    }

    private String getPlaceMessage(String replacement) {
        String[] messages = new String[] {
                "I just placed " + replacement + " blocks thanks to " + watermark.getValue() + "!",
                "Ich habe gerade " + replacement + " Blöcke dank " + watermark.getValue() + " platziert!",
                "Je viens de placer " + replacement + " blocs grâce à " + watermark.getValue() + "!",
                watermark.getValue() + "のおかげで" + replacement + "個のブロックを置いたところだ!",
                "Sijoitin juuri " + replacement + " lohkoa " + watermark.getValue() + " ansiosta!",
                "Я только что разместил " + replacement + " блоков благодаря " + watermark.getValue() + "!",
                "¡Acabo de colocar " + replacement + " bloques gracias a " + watermark.getValue() + "!",
                "Jag har precis placerat " + replacement + " block tack vare " + watermark.getValue() + "!",
                "Az önce " + watermark.getValue() + " sayesinde " + replacement + " blok yerleştirdim!",
                "Ik heb net " + replacement + " blokken geplaatst dankzij " + watermark.getValue() + "!",
                "Μόλις τοποθέτησα " + replacement + " μπλοκ χάρη στο " + watermark.getValue() + "!",
                "多亏了 " + watermark.getValue() + "，我刚刚放了 " + replacement + " 块!",
                "Ho appena piazzato " + replacement + " blocchi grazie a " + watermark.getValue() + "!",
                "Jeg har nettopp plassert " + replacement + " blokker takket være " + watermark.getValue() + "!",
                "Tocmai am plasat " + replacement + " blocuri datorită lui " + watermark.getValue() + "!",
                "Právě jsem umístil " + replacement + " bloků díky " + watermark.getValue() + "!",
                "Acabei de colocar " + replacement + " blocos graças ao " + watermark.getValue() + "!",
                "Pravkar sem postavil " + replacement + " blokov zahvaljujoč " + watermark.getValue() + "!",
                "Właśnie umieściłem " + replacement + " bloków dzięki " + watermark.getValue() + "!",
                watermark.getValue() + " 덕분에 방금 XXX 블록을 배치했습니다!",
                "Ką tik įdėjau " + replacement + " blokų dėka " + watermark.getValue() + "!",
                "Saya baru saja menempatkan blok " + replacement + " berkat " + watermark.getValue() + "!",
                "Most helyeztem el " + replacement + " blokkot a " + watermark.getValue() + "-nek köszönhetően!"
        };

        return messages[getLanguageIndex()];
    }

    private String getEatMessage(String replacement) {
        String[] messages = new String[] {
                "I just ate " + replacement + " thanks to " + watermark.getValue() + "!",
                "Ich habe gerade " + replacement + " gegessen, dank " + watermark.getValue() + "!",
                "Je viens de manger " + replacement + " grâce à " + watermark.getValue() + " !",
                watermark.getValue() + "のおかげで" + replacement + "を食べたよ!",
                "Söin juuri " + replacement + " kiitos " + watermark.getValue() + "!",
                "Я только что съел " + replacement + " благодаря " + watermark.getValue() + "!",
                "¡Acabo de comer " + replacement + " gracias a " + watermark.getValue() + "!",
                "Jag åt just " + replacement + " tack vare " + watermark.getValue() + "!",
                "Az önce " + watermark.getValue() + " sayesinde " + replacement + " yedim!",
                "Ik heb net " + replacement + " gegeten dankzij " + watermark.getValue() + "!",
                "Μόλις έφαγα " + replacement + " χάρη στο " + watermark.getValue() + "!",
                "我刚吃了 " + replacement + "，多亏了 " + watermark.getValue() + "!",
                "Ho appena mangiato " + replacement + " grazie a " + watermark.getValue() + "!",
                "Jeg spiste nettopp " + replacement + " takket være " + watermark.getValue() + "!",
                "Právě jsem snědl " + replacement + " díky " + watermark.getValue() + "!",
                "Acabei de comer " + replacement + " graças a " + watermark.getValue() + "!",
                "Pravkar sem pojedel " + replacement + " zaradi " + watermark.getValue() + "!",
                "Właśnie zjadłem " + replacement + " dzięki " + watermark.getValue() + "!",
                "방금 " + watermark.getValue() + " 덕분에 " + replacement + "를 먹었어요!",
                "Ką tik suvalgiau " + replacement + " dėl " + watermark.getValue() + "!",
                "Saya baru saja makan " + replacement + " berkat " + watermark.getValue() + "!",
                "Most ettem " + replacement + "-t, hála " + watermark.getValue() + "-nek!"
        };

        return messages[getLanguageIndex()];
    }

    private int getLanguageIndex() {
        return switch (language.getValue()) {
            case "German" -> 1;
            case "French" -> 2;
            case "Japanese" -> 3;
            case "Finnish" -> 4;
            case "Russian" -> 5;
            case "Spanish" -> 6;
            case "Swedish" -> 7;
            case "Turkish" -> 8;
            case "Dutch" -> 9;
            case "Greek" -> 10;
            case "Chinese" -> 11;
            case "Italian" -> 12;
            case "Norwegian" -> 13;
            case "Romanian" -> 14;
            case "Czech" -> 15;
            case "Portuguese" -> 16;
            case "Slovenian" -> 17;
            case "Polish" -> 18;
            case "Korean" -> 19;
            case "Lithuanian" -> 20;
            case "Indonesian" -> 21;
            case "Hungarian" -> 22;
            case "Random" -> (int) MathUtils.random(22, 0);
            default -> 0;
        };
    }
}
