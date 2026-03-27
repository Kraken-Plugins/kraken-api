package com.kraken.api.core.packet;


/**
 * This class is a direct copy of the ObfuscatedNames class from the EthanVann PacketUtils class found here:
 * https://github.com/Ethan-Vann/PacketUtils/blob/master/src/main/java/com/example/PacketUtils/ObfuscatedNames.java
 * The copy was made so that this API could remain fundamentally compatible with the popular EthanVann PacketUtils plugin.
 * <p>
 * This class contains all the definitions which map arguments for sending specific packets and client level packet related classes
 * to their obfuscated names. By utilizing this class we can use reflection to send packets through the game client by invoking the
 * right obfuscated methods with arguments.
 *
 * TODO Maintainer may be stepping away so should move this over to using VitaLite mappings
 *  <a href="https://github.com/Tonic-Box/VitaLite/blob/main/src/main/resources/com/tonic/injector/mappings.json">here</a>
 */
public final class ObfuscatedNames {

    public static final String EVENT_MOUSE_CLICK_OBFUSCATEDNAME = "aw";
    public static final String EVENT_MOUSE_CLICK_WRITE1 = "mouseX";
    public static final String EVENT_MOUSE_CLICK_METHOD_NAME1 = "eu";
    public static final String EVENT_MOUSE_CLICK_WRITE2 = "0";
    public static final String EVENT_MOUSE_CLICK_METHOD_NAME2 = "dn";
    public static final String EVENT_MOUSE_CLICK_WRITE3 = "mouseInfo";
    public static final String EVENT_MOUSE_CLICK_METHOD_NAME3 = "eu";
    public static final String EVENT_MOUSE_CLICK_WRITE4 = "mouseY";
    public static final String EVENT_MOUSE_CLICK_METHOD_NAME4 = "eh";
    public static final String[][] EVENT_MOUSE_CLICK_WRITES = new String[][]{
            {"v", "r 8"},
            {"s 0"},
            {"v", "r 8"},
            {"r 8", "v"},
    };
    public static final String IF_BUTTONT_OBFUSCATEDNAME = "by";
    public static final String IF_BUTTONT_WRITE1 = "sourceSlot";
    public static final String IF_BUTTONT_METHOD_NAME1 = "eh";
    public static final String IF_BUTTONT_WRITE2 = "destinationItemId";
    public static final String IF_BUTTONT_METHOD_NAME2 = "eh";
    public static final String IF_BUTTONT_WRITE3 = "destinationWidgetId";
    public static final String IF_BUTTONT_METHOD_NAME3 = "ck";
    public static final String IF_BUTTONT_WRITE4 = "destinationSlot";
    public static final String IF_BUTTONT_METHOD_NAME4 = "eh";
    public static final String IF_BUTTONT_WRITE5 = "sourceItemId";
    public static final String IF_BUTTONT_METHOD_NAME5 = "eh";
    public static final String IF_BUTTONT_WRITE6 = "sourceWidgetId";
    public static final String IF_BUTTONT_METHOD_NAME6 = "ek";
    public static final String[][] IF_BUTTONT_WRITES = new String[][]{
            {"r 8", "v"},
            {"r 8", "v"},
            {"r 24", "r 16", "r 8", "v"},
            {"r 8", "v"},
            {"r 8", "v"},
            {"v", "r 8", "r 16", "r 24"},
    };
    public static final String IF_BUTTONX_OBFUSCATEDNAME = "eq";
    public static final String IF_BUTTONX_WRITE1 = "widgetId";
    public static final String IF_BUTTONX_METHOD_NAME1 = "ck";
    public static final String IF_BUTTONX_WRITE2 = "slot";
    public static final String IF_BUTTONX_METHOD_NAME2 = "eh";
    public static final String IF_BUTTONX_WRITE3 = "itemId";
    public static final String IF_BUTTONX_METHOD_NAME3 = "eh";
    public static final String IF_BUTTONX_WRITE4 = "opCode";
    public static final String IF_BUTTONX_METHOD_NAME4 = "bw";
    public static final String[][] IF_BUTTONX_WRITES = new String[][]{
            {"r 24", "r 16", "r 8", "v"},
            {"r 8", "v"},
            {"r 8", "v"},
            {"v"},
    };
    public static final String IF_SUBOP_OBFUSCATEDNAME = "ef";
    public static final String IF_SUBOP_WRITE1 = "widgetId";
    public static final String IF_SUBOP_METHOD_NAME1 = "ck";
    public static final String IF_SUBOP_WRITE2 = "slot";
    public static final String IF_SUBOP_METHOD_NAME2 = "eh";
    public static final String IF_SUBOP_WRITE3 = "itemId";
    public static final String IF_SUBOP_METHOD_NAME3 = "eh";
    public static final String IF_SUBOP_WRITE4 = "menuIndex";
    public static final String IF_SUBOP_METHOD_NAME4 = "bw";
    public static final String IF_SUBOP_WRITE5 = "subActionIndex";
    public static final String IF_SUBOP_METHOD_NAME5 = "bw";
    public static final String[][] IF_SUBOP_WRITES = new String[][]{
            {"r 24", "r 16", "r 8", "v"},
            {"r 8", "v"},
            {"r 8", "v"},
            {"v"},
            {"v"},
    };
    public static final String MOVE_GAMECLICK_OBFUSCATEDNAME = "aa";
    public static final String MOVE_GAMECLICK_WRITE1 = "5";
    public static final String MOVE_GAMECLICK_METHOD_NAME1 = "bw";
    public static final String MOVE_GAMECLICK_WRITE2 = "ctrlDown";
    public static final String MOVE_GAMECLICK_METHOD_NAME2 = "bw";
    public static final String MOVE_GAMECLICK_WRITE3 = "worldPointX";
    public static final String MOVE_GAMECLICK_METHOD_NAME3 = "ec";
    public static final String MOVE_GAMECLICK_WRITE4 = "worldPointY";
    public static final String MOVE_GAMECLICK_METHOD_NAME4 = "eu";
    public static final String[][] MOVE_GAMECLICK_WRITES = new String[][]{
            {"v"},
            {"v"},
            {"a 128", "r 8"},
            {"v", "r 8"},
    };
    public static final String OPLOC1_OBFUSCATEDNAME = "df";
    public static final String OPLOC1_WRITE1 = "worldPointY";
    public static final String OPLOC1_METHOD_NAME1 = "eu";
    public static final String OPLOC1_WRITE2 = "subop";
    public static final String OPLOC1_METHOD_NAME2 = "dn";
    public static final String OPLOC1_WRITE3 = "objectId";
    public static final String OPLOC1_METHOD_NAME3 = "eu";
    public static final String OPLOC1_WRITE4 = "ctrlDown";
    public static final String OPLOC1_METHOD_NAME4 = "dp";
    public static final String OPLOC1_WRITE5 = "worldPointX";
    public static final String OPLOC1_METHOD_NAME5 = "eh";
    public static final String[][] OPLOC1_WRITES = new String[][]{
            {"v", "r 8"},
            {"s 0"},
            {"v", "r 8"},
            {"s 128"},
            {"r 8", "v"},
    };
    public static final String OPLOC2_OBFUSCATEDNAME = "dv";
    public static final String OPLOC2_WRITE1 = "objectId";
    public static final String OPLOC2_METHOD_NAME1 = "dw";
    public static final String OPLOC2_WRITE2 = "worldPointY";
    public static final String OPLOC2_METHOD_NAME2 = "ec";
    public static final String OPLOC2_WRITE3 = "worldPointX";
    public static final String OPLOC2_METHOD_NAME3 = "dw";
    public static final String OPLOC2_WRITE4 = "subop";
    public static final String OPLOC2_METHOD_NAME4 = "cp";
    public static final String OPLOC2_WRITE5 = "ctrlDown";
    public static final String OPLOC2_METHOD_NAME5 = "dn";
    public static final String[][] OPLOC2_WRITES = new String[][]{
            {"r 8", "a 128"},
            {"a 128", "r 8"},
            {"r 8", "a 128"},
            {"a 128"},
            {"s 0"},
    };
    public static final String OPLOC3_OBFUSCATEDNAME = "aq";
    public static final String OPLOC3_WRITE1 = "worldPointY";
    public static final String OPLOC3_METHOD_NAME1 = "ec";
    public static final String OPLOC3_WRITE2 = "ctrlDown";
    public static final String OPLOC3_METHOD_NAME2 = "bw";
    public static final String OPLOC3_WRITE3 = "worldPointX";
    public static final String OPLOC3_METHOD_NAME3 = "dw";
    public static final String OPLOC3_WRITE4 = "objectId";
    public static final String OPLOC3_METHOD_NAME4 = "eu";
    public static final String OPLOC3_WRITE5 = "subop";
    public static final String OPLOC3_METHOD_NAME5 = "cp";
    public static final String[][] OPLOC3_WRITES = new String[][]{
            {"a 128", "r 8"},
            {"v"},
            {"r 8", "a 128"},
            {"v", "r 8"},
            {"a 128"},
    };
    public static final String OPLOC4_OBFUSCATEDNAME = "cz";
    public static final String OPLOC4_WRITE1 = "subop";
    public static final String OPLOC4_METHOD_NAME1 = "dn";
    public static final String OPLOC4_WRITE2 = "worldPointX";
    public static final String OPLOC4_METHOD_NAME2 = "dw";
    public static final String OPLOC4_WRITE3 = "ctrlDown";
    public static final String OPLOC4_METHOD_NAME3 = "cp";
    public static final String OPLOC4_WRITE4 = "objectId";
    public static final String OPLOC4_METHOD_NAME4 = "ec";
    public static final String OPLOC4_WRITE5 = "worldPointY";
    public static final String OPLOC4_METHOD_NAME5 = "dw";
    public static final String[][] OPLOC4_WRITES = new String[][]{
            {"s 0"},
            {"r 8", "a 128"},
            {"a 128"},
            {"a 128", "r 8"},
            {"r 8", "a 128"},
    };
    public static final String OPLOC5_OBFUSCATEDNAME = "au";
    public static final String OPLOC5_WRITE1 = "objectId";
    public static final String OPLOC5_METHOD_NAME1 = "eu";
    public static final String OPLOC5_WRITE2 = "worldPointX";
    public static final String OPLOC5_METHOD_NAME2 = "ec";
    public static final String OPLOC5_WRITE3 = "worldPointY";
    public static final String OPLOC5_METHOD_NAME3 = "ec";
    public static final String OPLOC5_WRITE4 = "subop";
    public static final String OPLOC5_METHOD_NAME4 = "dp";
    public static final String OPLOC5_WRITE5 = "ctrlDown";
    public static final String OPLOC5_METHOD_NAME5 = "bw";
    public static final String[][] OPLOC5_WRITES = new String[][]{
            {"v", "r 8"},
            {"a 128", "r 8"},
            {"a 128", "r 8"},
            {"s 128"},
            {"v"},
    };
    public static final String OPLOCT_OBFUSCATEDNAME = "dx";
    public static final String OPLOCT_WRITE1 = "objectId";
    public static final String OPLOCT_WRITE2 = "ctrlDown";
    public static final String OPLOCT_WRITE3 = "worldPointY";
    public static final String OPLOCT_WRITE4 = "slot";
    public static final String OPLOCT_WRITE5 = "itemId";
    public static final String OPLOCT_WRITE6 = "widgetId";
    public static final String OPLOCT_WRITE7 = "worldPointX";
    public static final String[][] OPLOCT_WRITES = new String[][]{
            {"r 8","a 128",},
            {"s 128",},
            {"a 128","r 8",},
            {"a 128","r 8",},
            {"a 128","r 8",},
            {"v","r 8","r 16","r 24",},
            {"r 8","v",},
    };
    public static final String OPNPC1_OBFUSCATEDNAME = "da";
    public static final String OPNPC1_WRITE1 = "npcIndex";
    public static final String OPNPC1_METHOD_NAME1 = "eu";
    public static final String OPNPC1_WRITE2 = "ctrlDown";
    public static final String OPNPC1_METHOD_NAME2 = "dn";
    public static final String OPNPC1_WRITE3 = "subop";
    public static final String OPNPC1_METHOD_NAME3 = "bw";
    public static final String[][] OPNPC1_WRITES = new String[][]{
            {"v", "r 8"},
            {"s 0"},
            {"v"},
    };
    public static final String OPNPC2_OBFUSCATEDNAME = "af";
    public static final String OPNPC2_WRITE1 = "ctrlDown";
    public static final String OPNPC2_METHOD_NAME1 = "cp";
    public static final String OPNPC2_WRITE2 = "subop";
    public static final String OPNPC2_METHOD_NAME2 = "cp";
    public static final String OPNPC2_WRITE3 = "npcIndex";
    public static final String OPNPC2_METHOD_NAME3 = "ec";
    public static final String[][] OPNPC2_WRITES = new String[][]{
            {"a 128"},
            {"a 128"},
            {"a 128", "r 8"},
    };
    public static final String OPNPC3_OBFUSCATEDNAME = "eu";
    public static final String OPNPC3_WRITE1 = "ctrlDown";
    public static final String OPNPC3_METHOD_NAME1 = "dn";
    public static final String OPNPC3_WRITE2 = "subop";
    public static final String OPNPC3_METHOD_NAME2 = "cp";
    public static final String OPNPC3_WRITE3 = "npcIndex";
    public static final String OPNPC3_METHOD_NAME3 = "ec";
    public static final String[][] OPNPC3_WRITES = new String[][]{
            {"s 0"},
            {"a 128"},
            {"a 128", "r 8"},
    };
    public static final String OPNPC4_OBFUSCATEDNAME = "dh";
    public static final String OPNPC4_WRITE1 = "subop";
    public static final String OPNPC4_METHOD_NAME1 = "dn";
    public static final String OPNPC4_WRITE2 = "npcIndex";
    public static final String OPNPC4_METHOD_NAME2 = "eu";
    public static final String OPNPC4_WRITE3 = "ctrlDown";
    public static final String OPNPC4_METHOD_NAME3 = "dn";
    public static final String[][] OPNPC4_WRITES = new String[][]{
            {"s 0"},
            {"v", "r 8"},
            {"s 0"},
    };
    public static final String OPNPC5_OBFUSCATEDNAME = "ec";
    public static final String OPNPC5_WRITE1 = "subop";
    public static final String OPNPC5_METHOD_NAME1 = "dn";
    public static final String OPNPC5_WRITE2 = "npcIndex";
    public static final String OPNPC5_METHOD_NAME2 = "dw";
    public static final String OPNPC5_WRITE3 = "ctrlDown";
    public static final String OPNPC5_METHOD_NAME3 = "dp";
    public static final String[][] OPNPC5_WRITES = new String[][]{
            {"s 0"},
            {"r 8", "a 128"},
            {"s 128"},
    };
    public static final String OPNPCT_OBFUSCATEDNAME = "cx";
    public static final String OPNPCT_WRITE1 = "npcIndex";
    public static final String OPNPCT_METHOD_NAME1 = "ec";
    public static final String OPNPCT_WRITE2 = "itemId";
    public static final String OPNPCT_METHOD_NAME2 = "ec";
    public static final String OPNPCT_WRITE3 = "widgetId";
    public static final String OPNPCT_METHOD_NAME3 = "es";
    public static final String OPNPCT_WRITE4 = "slot";
    public static final String OPNPCT_METHOD_NAME4 = "eu";
    public static final String OPNPCT_WRITE5 = "ctrlDown";
    public static final String OPNPCT_METHOD_NAME5 = "cp";
    public static final String[][] OPNPCT_WRITES = new String[][]{
            {"a 128", "r 8"},
            {"a 128", "r 8"},
            {"r 16", "r 24", "v", "r 8"},
            {"v", "r 8"},
            {"a 128"},
    };
    public static final String OPOBJ1_OBFUSCATEDNAME = "bp";
    public static final String OPOBJ1_WRITE1 = "subop";
    public static final String OPOBJ1_METHOD_NAME1 = "cp";
    public static final String OPOBJ1_WRITE2 = "ctrlDown";
    public static final String OPOBJ1_METHOD_NAME2 = "cp";
    public static final String OPOBJ1_WRITE3 = "worldPointY";
    public static final String OPOBJ1_METHOD_NAME3 = "eu";
    public static final String OPOBJ1_WRITE4 = "objectId";
    public static final String OPOBJ1_METHOD_NAME4 = "eu";
    public static final String OPOBJ1_WRITE5 = "worldPointX";
    public static final String OPOBJ1_METHOD_NAME5 = "eu";
    public static final String[][] OPOBJ1_WRITES = new String[][]{
            {"a 128"},
            {"a 128"},
            {"v", "r 8"},
            {"v", "r 8"},
            {"v", "r 8"},
    };
    public static final String OPOBJ2_OBFUSCATEDNAME = "cm";
    public static final String OPOBJ2_WRITE1 = "ctrlDown";
    public static final String OPOBJ2_METHOD_NAME1 = "bw";
    public static final String OPOBJ2_WRITE2 = "objectId";
    public static final String OPOBJ2_METHOD_NAME2 = "eh";
    public static final String OPOBJ2_WRITE3 = "worldPointX";
    public static final String OPOBJ2_METHOD_NAME3 = "eu";
    public static final String OPOBJ2_WRITE4 = "subop";
    public static final String OPOBJ2_METHOD_NAME4 = "dp";
    public static final String OPOBJ2_WRITE5 = "worldPointY";
    public static final String OPOBJ2_METHOD_NAME5 = "eh";
    public static final String[][] OPOBJ2_WRITES = new String[][]{
            {"v"},
            {"r 8", "v"},
            {"v", "r 8"},
            {"s 128"},
            {"r 8", "v"},
    };
    public static final String OPOBJ3_OBFUSCATEDNAME = "ei";
    public static final String OPOBJ3_WRITE1 = "subop";
    public static final String OPOBJ3_METHOD_NAME1 = "dn";
    public static final String OPOBJ3_WRITE2 = "ctrlDown";
    public static final String OPOBJ3_METHOD_NAME2 = "bw";
    public static final String OPOBJ3_WRITE3 = "worldPointX";
    public static final String OPOBJ3_METHOD_NAME3 = "ec";
    public static final String OPOBJ3_WRITE4 = "worldPointY";
    public static final String OPOBJ3_METHOD_NAME4 = "ec";
    public static final String OPOBJ3_WRITE5 = "objectId";
    public static final String OPOBJ3_METHOD_NAME5 = "eu";
    public static final String[][] OPOBJ3_WRITES = new String[][]{
            {"s 0"},
            {"v"},
            {"a 128", "r 8"},
            {"a 128", "r 8"},
            {"v", "r 8"},
    };
    public static final String OPOBJ4_OBFUSCATEDNAME = "dn";
    public static final String OPOBJ4_WRITE1 = "subop";
    public static final String OPOBJ4_METHOD_NAME1 = "dp";
    public static final String OPOBJ4_WRITE2 = "worldPointX";
    public static final String OPOBJ4_METHOD_NAME2 = "eu";
    public static final String OPOBJ4_WRITE3 = "worldPointY";
    public static final String OPOBJ4_METHOD_NAME3 = "eh";
    public static final String OPOBJ4_WRITE4 = "ctrlDown";
    public static final String OPOBJ4_METHOD_NAME4 = "dn";
    public static final String OPOBJ4_WRITE5 = "objectId";
    public static final String OPOBJ4_METHOD_NAME5 = "eh";
    public static final String[][] OPOBJ4_WRITES = new String[][]{
            {"s 128"},
            {"v", "r 8"},
            {"r 8", "v"},
            {"s 0"},
            {"r 8", "v"},
    };
    public static final String OPOBJ5_OBFUSCATEDNAME = "bv";
    public static final String OPOBJ5_WRITE1 = "ctrlDown";
    public static final String OPOBJ5_METHOD_NAME1 = "dn";
    public static final String OPOBJ5_WRITE2 = "worldPointY";
    public static final String OPOBJ5_METHOD_NAME2 = "eu";
    public static final String OPOBJ5_WRITE3 = "worldPointX";
    public static final String OPOBJ5_METHOD_NAME3 = "eu";
    public static final String OPOBJ5_WRITE4 = "objectId";
    public static final String OPOBJ5_METHOD_NAME4 = "eh";
    public static final String OPOBJ5_WRITE5 = "subop";
    public static final String OPOBJ5_METHOD_NAME5 = "cp";
    public static final String[][] OPOBJ5_WRITES = new String[][]{
            {"s 0"},
            {"v", "r 8"},
            {"v", "r 8"},
            {"r 8", "v"},
            {"a 128"},
    };
    public static final String OPOBJT_OBFUSCATEDNAME = "cb";
    public static final String OPOBJT_WRITE1 = "slot";
    public static final String OPOBJT_WRITE2 = "objectId";
    public static final String OPOBJT_WRITE3 = "worldPointY";
    public static final String OPOBJT_WRITE4 = "widgetId";
    public static final String OPOBJT_WRITE5 = "worldPointX";
    public static final String OPOBJT_WRITE6 = "itemId";
    public static final String OPOBJT_WRITE7 = "ctrlDown";
    public static final String[][] OPOBJT_WRITES = new String[][]{
            {"a 128","r 8",},
            {"a 128","r 8",},
            {"a 128","r 8",},
            {"r 8","v","r 24","r 16",},
            {"r 8","v",},
            {"a 128","r 8",},
            {"a 128",},
    };
    public static final String OPPLAYER1_OBFUSCATEDNAME = "dm";
    public static final String OPPLAYER1_WRITE1 = "ctrlDown";
    public static final String OPPLAYER1_METHOD_NAME1 = "cp";
    public static final String OPPLAYER1_WRITE2 = "playerIndex";
    public static final String OPPLAYER1_METHOD_NAME2 = "eu";
    public static final String[][] OPPLAYER1_WRITES = new String[][]{
            {"a 128"},
            {"v", "r 8"},
    };
    public static final String OPPLAYER2_OBFUSCATEDNAME = "bw";
    public static final String OPPLAYER2_WRITE1 = "ctrlDown";
    public static final String OPPLAYER2_METHOD_NAME1 = "dp";
    public static final String OPPLAYER2_WRITE2 = "playerIndex";
    public static final String OPPLAYER2_METHOD_NAME2 = "ec";
    public static final String[][] OPPLAYER2_WRITES = new String[][]{
            {"s 128"},
            {"a 128", "r 8"},
    };
    public static final String OPPLAYER3_OBFUSCATEDNAME = "cj";
    public static final String OPPLAYER3_WRITE1 = "ctrlDown";
    public static final String OPPLAYER3_METHOD_NAME1 = "dp";
    public static final String OPPLAYER3_WRITE2 = "playerIndex";
    public static final String OPPLAYER3_METHOD_NAME2 = "eu";
    public static final String[][] OPPLAYER3_WRITES = new String[][]{
            {"s 128"},
            {"v", "r 8"},
    };
    public static final String OPPLAYER4_OBFUSCATEDNAME = "bx";
    public static final String OPPLAYER4_WRITE1 = "playerIndex";
    public static final String OPPLAYER4_METHOD_NAME1 = "dw";
    public static final String OPPLAYER4_WRITE2 = "ctrlDown";
    public static final String OPPLAYER4_METHOD_NAME2 = "bw";
    public static final String[][] OPPLAYER4_WRITES = new String[][]{
            {"r 8", "a 128"},
            {"v"},
    };
    public static final String OPPLAYER5_OBFUSCATEDNAME = "bc";
    public static final String OPPLAYER5_WRITE1 = "playerIndex";
    public static final String OPPLAYER5_METHOD_NAME1 = "dw";
    public static final String OPPLAYER5_WRITE2 = "ctrlDown";
    public static final String OPPLAYER5_METHOD_NAME2 = "dn";
    public static final String[][] OPPLAYER5_WRITES = new String[][]{
            {"r 8", "a 128"},
            {"s 0"},
    };
    public static final String OPPLAYER6_OBFUSCATEDNAME = "ce";
    public static final String OPPLAYER6_WRITE1 = "playerIndex";
    public static final String OPPLAYER6_METHOD_NAME1 = "eu";
    public static final String OPPLAYER6_WRITE2 = "ctrlDown";
    public static final String OPPLAYER6_METHOD_NAME2 = "cp";
    public static final String[][] OPPLAYER6_WRITES = new String[][]{
            {"v", "r 8"},
            {"a 128"},
    };
    public static final String OPPLAYER7_OBFUSCATEDNAME = "cp";
    public static final String OPPLAYER7_WRITE1 = "ctrlDown";
    public static final String OPPLAYER7_METHOD_NAME1 = "bw";
    public static final String OPPLAYER7_WRITE2 = "playerIndex";
    public static final String OPPLAYER7_METHOD_NAME2 = "dw";
    public static final String[][] OPPLAYER7_WRITES = new String[][]{
            {"v"},
            {"r 8", "a 128"},
    };
    public static final String OPPLAYER8_OBFUSCATEDNAME = "bo";
    public static final String OPPLAYER8_WRITE1 = "ctrlDown";
    public static final String OPPLAYER8_METHOD_NAME1 = "cp";
    public static final String OPPLAYER8_WRITE2 = "playerIndex";
    public static final String OPPLAYER8_METHOD_NAME2 = "eu";
    public static final String[][] OPPLAYER8_WRITES = new String[][]{
            {"a 128"},
            {"v", "r 8"},
    };
    public static final String OPPLAYERT_OBFUSCATEDNAME = "ca";
    public static final String OPPLAYERT_WRITE1 = "itemId";
    public static final String OPPLAYERT_METHOD_NAME1 = "dw";
    public static final String OPPLAYERT_WRITE2 = "slot";
    public static final String OPPLAYERT_METHOD_NAME2 = "eu";
    public static final String OPPLAYERT_WRITE3 = "ctrlDown";
    public static final String OPPLAYERT_METHOD_NAME3 = "dn";
    public static final String OPPLAYERT_WRITE4 = "widgetId";
    public static final String OPPLAYERT_METHOD_NAME4 = "eo";
    public static final String OPPLAYERT_WRITE5 = "playerIndex";
    public static final String OPPLAYERT_METHOD_NAME5 = "eh";
    public static final String[][] OPPLAYERT_WRITES = new String[][]{
            {"r 8", "a 128"},
            {"v", "r 8"},
            {"s 0"},
            {"r 8", "v", "r 24", "r 16"},
            {"r 8", "v"},
    };
    public static final String RESUME_COUNTDIALOG_OBFUSCATEDNAME = "bj";
    public static final String RESUME_COUNTDIALOG_WRITE1 = "var0";
    public static final String RESUME_COUNTDIALOG_METHOD_NAME1 = "ck";
    public static final String[][] RESUME_COUNTDIALOG_WRITES = new String[][]{
            {"r 24", "r 16", "r 8", "v"},
    };
    public static final String RESUME_NAMEDIALOG_OBFUSCATED_NAME = "ai";
    public static final String RESUME_NAMEDIALOG_WRITE1 = "length";
    public static final String RESUME_NAMEDIALOG_METHOD_NAME1 = "writeByte";
    public static final String RESUME_NAMEDIALOG_WRITE2 = "string";
    public static final String RESUME_NAMEDIALOG_METHOD_NAME2 = "writeStringCp1252NullTerminated";
    public static final String[][] RESUME_NAMEDIALOG_WRITES = new String[][] {
            {"v"},
            {"strn"},
    };
    public static final String RESUME_OBJDIALOG_OBFUSCATEDNAME = "ck";
    public static final String RESUME_OBJDIALOG_WRITE1 = "var0";
    public static final String RESUME_OBJDIALOG_METHOD_NAME1 = "ca";
    public static final String[][] RESUME_OBJDIALOG_WRITES = new String[][]{
            {"r 8", "v"},
    };
    public static final String RESUME_PAUSEBUTTON_OBFUSCATEDNAME = "en";
    public static final String RESUME_PAUSEBUTTON_WRITE1 = "var1";
    public static final String RESUME_PAUSEBUTTON_METHOD_NAME1 = "eh";
    public static final String RESUME_PAUSEBUTTON_WRITE2 = "var0";
    public static final String RESUME_PAUSEBUTTON_METHOD_NAME2 = "eo";
    public static final String[][] RESUME_PAUSEBUTTON_WRITES = new String[][]{
            {"r 8", "v"},
            {"r 8", "v", "r 24", "r 16"},
    };

    public static final String OPHELDD_OBFUSCATEDNAME = "dy";
    public static final String OPHELDD_WRITE1 = "destChildIndex";
    public static final String OPHELDD_METHOD_NAME1 = "dw";
    public static final String OPHELDD_WRITE2 = "selectedChildIndex";
    public static final String OPHELDD_METHOD_NAME2 = "dw";
    public static final String OPHELDD_WRITE3 = "selectedId";
    public static final String OPHELDD_METHOD_NAME3 = "eo";
    public static final String OPHELDD_WRITE4 = "destId";
    public static final String OPHELDD_METHOD_NAME4 = "eo";
    public static final String OPHELDD_WRITE5 = "selectedItemId";
    public static final String OPHELDD_METHOD_NAME5 = "eu";
    public static final String OPHELDD_WRITE6 = "destItemId";
    public static final String OPHELDD_METHOD_NAME6 = "dw";
    public static final String[][] OPHELDD_WRITES = new String[][]{
            {"r 8", "a 128"},
            {"r 8", "a 128"},
            {"r 8", "v", "r 24", "r 16"},
            {"r 8", "v", "r 24", "r 16"},
            {"v", "r 8"},
            {"r 8", "a 128"},
    };
    public static final String RESUME_STRINGDIALOG_OBFUSCATED_NAME = "ai";
    public static final String RESUME_STRINGDIALOG_WRITE1 = "length";
    public static final String RESUME_STRINGDIALOG_METHOD_NAME1 = "writeByte";
    public static final String RESUME_STRINGDIALOG_WRITE2 = "string";
    public static final String RESUME_STRINGDIALOG_METHOD_NAME2 = "writeStringCp1252NullTerminated";
    public static final String[][] RESUME_STRINGDIALOG_WRITES = new String[][] {
            {"v"},
            {"strn"},
    };
    public static final String SET_HEADING_OBFUSCATEDNAME = "cf";
    public static final String SET_HEADING_WRITE1 = "orientation";
    public static final String SET_HEADING_METHOD_NAME1 = "dp";
    public static final String[][] SET_HEADING_WRITES = new String[][]{
            {"s 128"},
    };
    public static final String offsetMultiplier = "1044177923";
    public static final String indexMultiplier = "-288102741";
    public static final String addNodeGarbageValue = "-123";
    public static final String getPacketBufferNodeGarbageValue = "34";
    public static final String packetWriterFieldName = "ay";
    public static final String isaacCipherFieldName = "az";
    public static final String addNodeMethodName = "ae";
    public static final String clientPacketClassName = "iq";
    public static final String packetWriterClassName = "di";
    public static final String classContainingGetPacketBufferNodeName = "hk";
    public static final String packetBufferNodeClassName = "jg";
    public static final String packetBufferFieldName = "ai";
    public static final String bufferOffsetField = "as";
    public static final String bufferArrayField = "am";
    public static final String mouseHandlerMillisMultiplier = "2959047111086142593";
    public static final String clientMillisMultiplier = "2332694692091611995";
    public static final String MouseHandler_lastPressedTimeMillisClass = "sj";
    public static final String MouseHandler_lastPressedTimeMillisField = "ab";
    public static final String clientMillisField = "gb";
    public static final int getAnimationMultiplier = 1389085133;
    public static final int skullIconMultiplier = 0;
    public static final String skullIconField = "null";
    public static final int pathLengthMultiplier = 0;
    public static final String doActionClassName = "ox";
    public static final String doActionMethodName = "ek";
}