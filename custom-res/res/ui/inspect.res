Haven Resource 1 src M  LocalInspect.java /* Preprocessed source code */
package haven.res.ui.inspect;

import haven.*;
import java.util.*;
import java.awt.Color;

/* >wdg: LocalInspect */
public class LocalInspect extends Widget {
    public MapView mv;
    public Hover last = null, cur = null;

    public static Widget mkwidget(UI ui, Object... args) {
	return(new LocalInspect());
    }

    protected void added() {
	super.added();
	mv = getparent(GameUI.class).map;
	move(Coord.z);
	resize(parent.sz);
    }

    public void destroy() {
	super.destroy();
    }

    public static class ObTip implements Indir<Tex> {
	public final String name;
	public final List<String> lines;

	public ObTip(String name, List<String> lines) {
	    this.name = name;
	    this.lines = lines;
	}

	public boolean equals(ObTip that) {
	    return(Utils.eq(this.name, that.name) && Utils.eq(this.lines, that.lines));
	}
	public boolean equals(Object x) {
	    return((x instanceof ObTip) && equals((ObTip)x));
	}

	private Tex tex;
	private boolean r = false;
	public Tex get() {
	    if(!r) {
		StringBuilder buf = new StringBuilder();
		if(name != null)
		    buf.append(RichText.Parser.quote(name));
		if(!lines.isEmpty()) {
		    if(buf.length() > 0)
			buf.append("\n\n");
		    for(String ln : lines)
			buf.append(RichText.Parser.quote(ln) + "\n");
		}
		if(buf.length() > 0)
		tex = new TexI(RichText.render(buf.toString(), 0).img);
		r = true;
	    }
	    return(tex);
	}
    }

    public class Hover extends MapView.Hittest {
	public volatile boolean done = false;
	public Coord2d mc;
	public ClickData inf;
	public Gob ob;
	public Object tip;

	public Hover(Coord c) {
	    mv.super(c);
	}

	protected void hit(Coord pc, Coord2d mc, ClickData inf) {
	    this.mc = mc;
	    this.inf = inf;
	    this.done = true;
	    if(inf != null) {
		for(Object o : inf.array()) {
		    if(o instanceof Gob) {
			ob = (Gob)o;
			break;
		    }
		}
	    }
	}

	protected void nohit(Coord pc) {
	    done = true;
	}

	public Object tip() {
	    if(ob != null) {
		String name = null;
		GobIcon icon = ob.getattr(GobIcon.class);
		if(icon != null) {
		    Resource.Tooltip otip = icon.res.get().layer(Resource.tooltip);
		    if(otip != null)
			name = otip.t;
		}
		SavedInfo cell = ob.getattr(SavedInfo.class);
		return(new ObTip(name, (cell == null) ? Collections.emptyList() : cell.lines));
	    }
	    if(mc != null) {
		int tid = ui.sess.glob.map.gettile(mc.floor(MCache.tilesz));
		Resource tile = ui.sess.glob.map.tilesetr(tid);
		Resource.Tooltip name = tile.layer(Resource.tooltip);
		if(name != null)
		    return(name.t);
	    }
	    return(null);
	}
    }

    public boolean active() {
	return(true);
    }

    public void tick(double dt) {
	super.tick(dt);
	if((cur != null) && cur.done) {
	    last = cur;
	    cur = null;
	}
	if(active()) {
	    if(cur == null) {
		Coord mvc = mv.rootxlate(ui.mc);
		if(mv.area().contains(mvc)) {
		    (cur = new Hover(mvc)).run();
		}
	    }
	} else {
	    last = null;
	}
    }

    public Object tooltip(Coord c, Widget prev) {
	if(active()) {
	    if(last != null)
		return(last.tip());
	}
	return(super.tooltip(c, prev));
    }
}

src M  SavedInfo.java /* Preprocessed source code */
package haven.res.ui.inspect;

import haven.*;
import java.util.*;
import java.awt.Color;

/* >wdg: LocalInspect */
public class SavedInfo extends GAttrib {
    public List<String> lines = Collections.emptyList();

    public SavedInfo(Gob gob) {
	super(gob);
    }
}

/* >msg: Info */
src 	  Info.java /* Preprocessed source code */
package haven.res.ui.inspect;

import haven.*;
import java.util.*;
import java.awt.Color;

/* >wdg: LocalInspect */
public class Info implements UI.Notice {
    public final long gobid;
    public final boolean syn;
    public final String text;

    public Info(long gobid, boolean syn, String text) {
	this.gobid = gobid;
	this.syn = syn;
	this.text = text;
    }

    public static UI.Notice mkmessage(OwnerContext owner, Object... args) {
	long gobid = Utils.uiv(args[0]);
	String text = (String)args[1];
	boolean syn = (args.length > 2) ? Utils.bv(args[2]) : false;
	return(new Info(gobid, syn, text));
    }

    public String message() {return(text);}
    public Color color() {return(Color.WHITE);}
    public Audio.Clip sfx() {return(UI.InfoMessage.sfx);}

    private void save(Glob glob) {
	Gob gob = glob.oc.getgob(gobid);
	if(gob != null) {
	    SavedInfo cell = gob.getattr(SavedInfo.class);
	    if(syn || (cell == null))
		gob.setattr(cell = new SavedInfo(gob));
	    List<String> lines = new ArrayList<>(cell.lines.size() + 1);
	    lines.addAll(cell.lines);
	    lines.add(text);
	    cell.lines = lines;
	}
    }

    public boolean handle(Widget w) {
	if(w instanceof GameUI)
	    save(w.ui.sess.glob);
	return(false);
    }
}
code ~  haven.res.ui.inspect.LocalInspect$ObTip Êþº¾   4 y
  :	  ;	  <	  =
 > ? A
  B C
  :
 D E
  F G H
  I J G K L M L N O P
  Q R S
 T U	 T V
  W	  X
  Y Z name Ljava/lang/String; lines Ljava/util/List; 	Signature $Ljava/util/List<Ljava/lang/String;>; tex Lhaven/Tex; r Z <init> %(Ljava/lang/String;Ljava/util/List;)V Code LineNumberTable 9(Ljava/lang/String;Ljava/util/List<Ljava/lang/String;>;)V equals ObTip InnerClasses ,(Lhaven/res/ui/inspect/LocalInspect$ObTip;)Z StackMapTable (Ljava/lang/Object;)Z get ()Lhaven/Tex; C [ ()Ljava/lang/Object; ,Ljava/lang/Object;Lhaven/Indir<Lhaven/Tex;>; 
SourceFile LocalInspect.java ' \ % &      ] ^ _ ` 'haven/res/ui/inspect/LocalInspect$ObTip , / java/lang/StringBuilder a c d e f g h i j k 

 l m [ n i o 6 java/lang/String 
 p q 
haven/TexI java/lang/Object r s t u v ' w # $ 2 3 haven/Indir java/util/Iterator ()V haven/Utils eq '(Ljava/lang/Object;Ljava/lang/Object;)Z !haven/res/ui/inspect/LocalInspect haven/RichText$Parser Parser quote &(Ljava/lang/String;)Ljava/lang/String; append -(Ljava/lang/String;)Ljava/lang/StringBuilder; java/util/List isEmpty ()Z length ()I iterator ()Ljava/util/Iterator; hasNext next toString ()Ljava/lang/String; haven/RichText render 8(Ljava/lang/String;I[Ljava/lang/Object;)Lhaven/RichText; img Ljava/awt/image/BufferedImage; !(Ljava/awt/image/BufferedImage;)V inspect.cjava !                !    "  # $    % &     ' (  )   <     *· *µ *+µ *,µ ±    *         , 	    !  " !    +  , /  )   E     "*´ +´ ¸  *´ +´ ¸  § ¬    0     @ *       %  , 1  )   ;     +Á  *+À ¶  § ¬    0    @ *       (  2 3  )       ¢*´  » Y· 	L*´ Æ +*´ ¸ 
¶ W*´ ¹   L+¶  
+¶ W*´ ¹  M,¹   +,¹  À N+» Y· 	-¸ 
¶ ¶ ¶ ¶ W§ÿÒ+¶  *» Y+¶ ½ ¸ ´ · µ *µ *´ °    0    ü " 4ü 	 5ú 0 ú  *   6    .  /  0  1 " 2 . 3 5 4 < 5 Y 6 w 8 ~ 9  :  <A 2 6  )        *¶ °    *         8    x !    7 .      @ - 	 D T b 	code 
  haven.res.ui.inspect.LocalInspect$Hover Êþº¾   4 «	 ! A	 B C
 D E
 " F	 ! G	 ! H	 ! I
 J K L	 ! M N
 	 O	  P Q R S	  T
  U V	  Y Z [
 ] ^	  _
  `	 B a	 b c	 d e	 f g	 h i
 j k
 h l
 h m n q done Z mc Lhaven/Coord2d; inf Lhaven/ClickData; ob Lhaven/Gob; tip Ljava/lang/Object; this$0 #Lhaven/res/ui/inspect/LocalInspect; <init> 3(Lhaven/res/ui/inspect/LocalInspect;Lhaven/Coord;)V Code LineNumberTable hit 0(Lhaven/Coord;Lhaven/Coord2d;Lhaven/ClickData;)V StackMapTable s nohit (Lhaven/Coord;)V ()Ljava/lang/Object; t N n Z u 
SourceFile LocalInspect.java - . v w x y z { / | # $ % & ' ( } ~  	haven/Gob ) * haven/GobIcon       9 haven/Resource     haven/Resource$Tooltip Tooltip InnerClasses   haven/res/ui/inspect/SavedInfo 'haven/res/ui/inspect/LocalInspect$ObTip ObTip      /                & ¡ ¢ £ ¤ ¥ ¦ § 'haven/res/ui/inspect/LocalInspect$Hover Hover ¨ haven/MapView$Hittest Hittest [Ljava/lang/Object; java/lang/String java/util/List !haven/res/ui/inspect/LocalInspect mv Lhaven/MapView; java/lang/Object getClass ()Ljava/lang/Class; (Lhaven/MapView;Lhaven/Coord;)V haven/ClickData array ()[Ljava/lang/Object; getattr "(Ljava/lang/Class;)Lhaven/GAttrib; res Lhaven/Indir; haven/Indir get tooltip Ljava/lang/Class; layer © Layer )(Ljava/lang/Class;)Lhaven/Resource$Layer; t Ljava/lang/String; java/util/Collections 	emptyList ()Ljava/util/List; lines Ljava/util/List; %(Ljava/lang/String;Ljava/util/List;)V ui 
Lhaven/UI; haven/UI sess Lhaven/Session; haven/Session glob Lhaven/Glob; 
haven/Glob map Lhaven/MCache; haven/MCache tilesz haven/Coord2d floor (Lhaven/Coord2d;)Lhaven/Coord; gettile (Lhaven/Coord;)I tilesetr (I)Lhaven/Resource; haven/MapView haven/Resource$Layer inspect.cjava ! ! "    A # $    % &    ' (    ) *    + ,   - .     / 0  1   =     *+µ *+´ Y¶ W,· *µ ±    2       G  H  A  I  3 4  1        J*,µ *-µ *µ -Æ 9-¶ :¾66¢ $2:Á 	 *À 	µ 
§ 	§ÿÛ±    5    þ ! 6!ø  2   * 
   L  M 
 N  O  P / Q 7 R @ S C P I W  7 8  1   "     *µ ±    2   
    Z  [  + 9  1  S     ­*´ 
Æ YL*´ 
¶ À M,Æ ",´ ¹  À ² ¶ À N-Æ -´ L*´ 
¶ À N» Y+-Ç 	¸ § -´ · °*´ Æ J*´ ´ ´ ´ ´ *´ ² ¶ ¶ <*´ ´ ´ ´ ´ ¶  M,² ¶ À N-Æ -´ °°    5   L ý 9 : ;ÿ   < : ; =  F F :ÿ   < : ; =  F F : >ø û M 2   B    ^  _ 	 `  a  b 0 c 4 d 9 f F g ] i d j  k  l ¢ m ¦ n « p  ?    ª X   *    W   B \ 	 ! B o  " p r   code w  haven.res.ui.inspect.LocalInspect Êþº¾   4 z
  9	  :	  ; <
  9
  = >
  ?	  @	  A	 B C
  D	  E	  F
  G
  H
  I	  J
  K	  L	 M N
 O P
 O Q
 R S T
  U
  V
  W
  X Y Hover InnerClasses Z ObTip mv Lhaven/MapView; last )Lhaven/res/ui/inspect/LocalInspect$Hover; cur <init> ()V Code LineNumberTable mkwidget -(Lhaven/UI;[Ljava/lang/Object;)Lhaven/Widget; added destroy active ()Z tick (D)V StackMapTable tooltip /(Lhaven/Coord;Lhaven/Widget;)Ljava/lang/Object; 
SourceFile LocalInspect.java ( ) % & ' & !haven/res/ui/inspect/LocalInspect . ) haven/GameUI [ \ ] $ # $ ^ _ ` a b c d e ` f b / ) 2 3 g h 0 1 i j k l ` m n o p q r s t 'haven/res/ui/inspect/LocalInspect$Hover ( u v ) w x 5 6 haven/Widget 'haven/res/ui/inspect/LocalInspect$ObTip 	getparent !(Ljava/lang/Class;)Lhaven/Widget; map haven/Coord z Lhaven/Coord; move (Lhaven/Coord;)V parent Lhaven/Widget; sz resize done Z ui 
Lhaven/UI; haven/UI mc haven/MapView 	rootxlate (Lhaven/Coord;)Lhaven/Coord; area ()Lhaven/Area; 
haven/Area contains (Lhaven/Coord;)Z 3(Lhaven/res/ui/inspect/LocalInspect;Lhaven/Coord;)V run tip ()Ljava/lang/Object; inspect.cjava !       # $    % &    ' &     ( )  *   +     *· *µ *µ ±    +   
      
  , -  *         » Y· °    +         . )  *   O     '*· **¶ À ´ 	µ 
*² ¶ **´ ´ ¶ ±    +              &   / )  *   !     *· ±    +   
        0 1  *        ¬    +       u  2 3  *   ¸     h*'· *´ Æ *´ ´  **´ µ *µ *¶  ;*´ Ç 9*´ 
*´ ´ ¶ N*´ 
¶ -¶  *» Y*-· Zµ ¶ § *µ ±    4    #; +   2    y  z  {  | # ~ *  1  @  N  _  b  g   5 6  *   J     *¶  *´ Æ *´ ¶ °*+,· °    4     +               7    y           !  " 	code Ê  haven.res.ui.inspect.SavedInfo Êþº¾   4 
  
  	     lines Ljava/util/List; 	Signature $Ljava/util/List<Ljava/lang/String;>; <init> (Lhaven/Gob;)V Code LineNumberTable 
SourceFile SavedInfo.java 
       haven/res/ui/inspect/SavedInfo haven/GAttrib java/util/Collections 	emptyList ()Ljava/util/List; inspect.cjava !              	   
      -     *+· *¸ µ ±                     code   haven.res.ui.inspect.Info Êþº¾   4 
  ?	  @	  A	  B
 C D E
 C F G
  H	 I J	 K L	 M N
 O P Q
 R S
  T
 R U V	  W X Y
  Z X [ X \ ]	 ^ _	 ` a	 b c
  d e f gobid J syn Z text Ljava/lang/String; <init> (JZLjava/lang/String;)V Code LineNumberTable 	mkmessage Notice InnerClasses :(Lhaven/OwnerContext;[Ljava/lang/Object;)Lhaven/UI$Notice; StackMapTable E message ()Ljava/lang/String; color ()Ljava/awt/Color; sfx h Clip ()Lhaven/Audio$Clip; save (Lhaven/Glob;)V i Q handle (Lhaven/Widget;)Z 
SourceFile 	Info.java % j    ! " # $ k l m java/lang/String n o haven/res/ui/inspect/Info % & p q r s 3 u v w x y z { haven/res/ui/inspect/SavedInfo i | } % ~   java/util/ArrayList      %     o haven/GameUI          7 8 java/lang/Object haven/UI$Notice  haven/Audio$Clip 	haven/Gob ()V haven/Utils uiv (Ljava/lang/Object;)J bv (Ljava/lang/Object;)Z java/awt/Color WHITE Ljava/awt/Color; haven/UI$InfoMessage InfoMessage Lhaven/Audio$Clip; 
haven/Glob oc Lhaven/OCache; haven/OCache getgob (J)Lhaven/Gob; getattr "(Ljava/lang/Class;)Lhaven/GAttrib; (Lhaven/Gob;)V setattr (Lhaven/GAttrib;)V lines Ljava/util/List; java/util/List size ()I (I)V addAll (Ljava/util/Collection;)Z add haven/Widget ui 
Lhaven/UI; haven/UI sess Lhaven/Session; haven/Session glob Lhaven/Glob; haven/Audio inspect.cjava !             ! "    # $     % &  '   =     *· *µ *µ *µ ±    (       ¡  ¢ 	 £  ¤  ¥  ) ,  '   c     .+2¸ A+2À :+¾¤ +2¸ § 6» Y · 	°    -    ý  .@ (       ¨  ©  ª ! «  / 0  '        *´ °    (       ®  1 2  '        ² 
°    (       ¯  3 6  '        ² °    (       °  7 8  '   ·     f+´ *´ ¶ M,Æ X,¶ À N*´  -Ç ,» Y,· YN¶ » Y-´ ¹  `· :-´ ¹  W*´ ¹  W-µ ±    -    ý % 9 :ú 1 (   * 
   ³  ´  µ  ¶ % · 3 ¸ G ¹ S º _ » e ½  ; <  '   @     +Á  *+´ ´ ´ · ¬    -     (       À  Á  Â  =     +      ` *	 4 g 5	 K ` t 	codeentry G   wdg haven.res.ui.inspect.LocalInspect msg haven.res.ui.inspect.Info   