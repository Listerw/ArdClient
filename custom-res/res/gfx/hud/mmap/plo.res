Haven Resource 1	 image �  d   ��    mm/rot   ��scale    @ �PNG

   IHDR           szz�   0iTXtComment     Player
scale: 2
mm/rot: -90.0
z: 100}N�  UIDATX��?h#G���tZ�e8�p�~�\�u�� ,D�g��Dlp
�ƥ�4��M�ps��	B��)�
��$���Vt��5�ݳ6ͨ���,�wM�5�̾��7ߛy3p�\J����ǮH� ���&���	x�����I@)����dsssd2��l懂��w ��fcQ2������ޞ�����Z�oE���})e�qR����R)�AJ���R�@���eY!D�!�eY��;Pu��K��K)�@lee�t:�����@ LMM111���~LJ�����V�ո� 9���y�a�m�Qض�a��y8�����u�����r$	��x������!� C��\ ��zv��R_�����< ��G�n�r�,vww)�ˢ����~`�b�=��Z�]b�� �RJ�ǣ0��b���V�����,--�!�x)�h�Z�������0��f�,�v�M��Đ�v���Z�f�I��Ʋ,���`�wF>J�o�π���Eqtt����(� �#�9�>�a�ѰR����������I)�{�V��Q� ����EQ�T�z�PV�֫f��^�S*�DQ>��]g�=����ess���������_�Z�/�j���kkk qg�=��Z�766 8<<� ����pa�1��&�P%x�ٶM�X��S�����n�?.��bd��@�G�&�v]�V���j���^�L�2v�j�J�������&p���B� �\k��0���^(����=�)�T*�z� x<���q��*��H��rr���ߍ�n�M���	�t�yݼ&�@�����}M��o�Ԓo���?�q^s�u    IEND�B`�tooltip    Playersrc �  Player.java /* Preprocessed source code */
/* $use: ui/obj/buddy */

package haven.res.gfx.hud.mmap.plo;

import haven.*;
import haven.res.ui.obj.buddy.*;
import java.util.*;
import java.awt.Color;
import java.awt.image.*;

public class Player extends GobIcon.Icon {
    public static final Resource.Image img = Resource.classres(Player.class).layer(Resource.imgc);
    public final Gob gob = owner.fcontext(Gob.class, false);
    public final int group;

    public Player(OwnerContext owner, Resource res, int group) {
	super(owner, res);
	this.group = group;
	if((group < 0) && (gob != null) && (group() < 0)) {
	    /* XXX? I'm not sure how nice this is, but it's better
	     * than mis-classifying players as the wrong group just
	     * because the buddy-info hasn't been resolved yet. */
	    throw(new Gob.DataLoading(gob, "Waiting for group-info..."));
	}
    }

    public Player(OwnerContext owner, Resource res) {
	this(owner, res, -1);
    }

    public int group() {
	Buddy buddy = gob.getattr(Buddy.class);
	if((buddy != null) && (buddy.buddy() != null))
	    return(buddy.buddy().group);
	return(-1);
    }

    public Object[] id() {
	int grp = (this.group >= 0) ? this.group : group();
	if(grp <= 0)
	    return(nilid);
	return(new Object[grp]);
    }

    public Color color() {
	int grp = group();
	if((grp >= 0) && (grp < BuddyWnd.gc.length))
	    return(BuddyWnd.gc[grp]);
	return(Color.WHITE);
    }

    public String name() {return("Player");}

    public BufferedImage image() {
	if(group < 0)
	    return(img.img);
	BufferedImage buf = PUtils.copy(img.img);
	PUtils.colmul(buf.getRaster(), BuddyWnd.gc[group]);
	return(buf);
    }

    public void draw(GOut g, Coord cc) {
	Color col = Utils.colmul(g.getcolor(), color());
	g.chcolor(col);
	g.rotimage(img.tex(), cc, img.ssz.div(2), -gob.a - (Math.PI * 0.5));
	g.chcolor();
    }

    public boolean checkhit(Coord c) {
	return(c.isect(img.ssz.div(2).inv(), img.ssz));
    }

    public int z() {return(img.z);}
}

/* >mapicon: Factory */
src }  Factory.java /* Preprocessed source code */
/* $use: ui/obj/buddy */

package haven.res.gfx.hud.mmap.plo;

import haven.*;
import haven.res.ui.obj.buddy.*;
import java.util.*;
import java.awt.Color;
import java.awt.image.*;

public class Factory implements GobIcon.Icon.Factory {
    public Player create(OwnerContext owner, Resource res, Message sdt) {
	return(new Player(owner, res));
    }

    public Collection<Player> enumerate(OwnerContext owner, Resource res, Message sdt) {
	Collection<Player> ret = new ArrayList<>();
	for(int i = 0; i < BuddyWnd.gc.length; i++)
	    ret.add(new Player(owner, res, i));
	return(ret);
    }
}
code 3  haven.res.gfx.hud.mmap.plo.Player ����   4 �
 / T	 * U V W X	 * Y	 * Z
 * [ \ ^
  _
 * ` a
  b
  c	 d Z	 * e f	 g h	 i j k	 * l	 . m
 n o
 p q
 n r
 s t
 * u
 v w
 s x
 . y	 . z
 { |	  } ~?�!�TD-
 s 
 s �
 { �
 { �	 . � �
 � �	 � �
 � � � � img Image InnerClasses Lhaven/Resource$Image; gob Lhaven/Gob; group I <init> ((Lhaven/OwnerContext;Lhaven/Resource;I)V Code LineNumberTable StackMapTable � � � '(Lhaven/OwnerContext;Lhaven/Resource;)V ()I a id ()[Ljava/lang/Object; color ()Ljava/awt/Color; name ()Ljava/lang/String; image  ()Ljava/awt/image/BufferedImage; draw (Lhaven/GOut;Lhaven/Coord;)V checkhit (Lhaven/Coord;)Z z <clinit> ()V 
SourceFile Player.java 8 @ � � 	haven/Gob � � � 4 5 6 7 6 A haven/Gob$DataLoading DataLoading Waiting for group-info... 8 � 8 9 haven/res/ui/obj/buddy/Buddy � � � � � � � java/lang/Object � � � � � � Player 0 3 0 � � � � � � � � � � � F E F � � � � � � � � � � � � � � java/lang/Math � � � Q � � � � O 7 !haven/res/gfx/hud/mmap/plo/Player � � � � � � � haven/Resource$Image � haven/GobIcon$Icon Icon haven/OwnerContext haven/Resource owner Lhaven/OwnerContext; fcontext &(Ljava/lang/Class;Z)Ljava/lang/Object;  (Lhaven/Gob;Ljava/lang/String;)V getattr "(Ljava/lang/Class;)Lhaven/GAttrib; buddy Buddy ()Lhaven/BuddyWnd$Buddy; haven/BuddyWnd$Buddy nilid [Ljava/lang/Object; haven/BuddyWnd gc [Ljava/awt/Color; java/awt/Color WHITE Ljava/awt/Color; Ljava/awt/image/BufferedImage; haven/PUtils copy >(Ljava/awt/image/BufferedImage;)Ljava/awt/image/BufferedImage; java/awt/image/BufferedImage 	getRaster !()Ljava/awt/image/WritableRaster; colmul P(Ljava/awt/image/WritableRaster;Ljava/awt/Color;)Ljava/awt/image/WritableRaster; 
haven/GOut getcolor haven/Utils 2(Ljava/awt/Color;Ljava/awt/Color;)Ljava/awt/Color; chcolor (Ljava/awt/Color;)V tex ()Lhaven/Tex; ssz Lhaven/Coord; haven/Coord div (I)Lhaven/Coord; a D rotimage )(Lhaven/Tex;Lhaven/Coord;Lhaven/Coord;D)V inv ()Lhaven/Coord; isect (Lhaven/Coord;Lhaven/Coord;)Z classres #(Ljava/lang/Class;)Lhaven/Resource; imgc Ljava/lang/Class; layer � Layer )(Ljava/lang/Class;)Lhaven/Resource$Layer; haven/GobIcon haven/Resource$Layer 	plo.cjava ! * /     0 3    4 5    6 7     8 9  :   �     ?*+,� **� �  � � *� � *� � *� � � Y*� 	� 
��    <    � >  = > ?   ;              0  >   8 @  :   $     *+,� �    ;   
        6 A  :   T     "*� � � L+� +� � +� � ��    <    �   B ;          !  "   #  C D  :   S      *� � 
*� � *� <� � �� �    <   	 C�  ;       '  (  )  *  E F  :   K     *� <� � �� 	� 2�� �    <    �  ;       .  /  0  1  G H  :        �    ;       4  I J  :   [     **� � 
� � �� � � L+� � *� 2� W+�    <     ;       7  8  9  : ( ;  K L  :   _     7+� *� � N+-� +� � ,� � �  *� � !w #g� %+� &�    ;       ?  @  A 2 B 6 C  M N  :   0     +� � �  � '� � � (�    ;       F  O A  :        � � )�    ;       I  P Q  :   *      *� +� ,� -� .� �    ;         R    � 2   *  . � 1    ] 	 / � �	 d g �  � � �code 7  haven.res.gfx.hud.mmap.plo.Factory ����   4 9
    
  ! "
  	 # $
  % & '
 
 ( ) * + <init> ()V Code LineNumberTable create X(Lhaven/OwnerContext;Lhaven/Resource;Lhaven/Message;)Lhaven/res/gfx/hud/mmap/plo/Player; 	enumerate K(Lhaven/OwnerContext;Lhaven/Resource;Lhaven/Message;)Ljava/util/Collection; StackMapTable - 	Signature p(Lhaven/OwnerContext;Lhaven/Resource;Lhaven/Message;)Ljava/util/Collection<Lhaven/res/gfx/hud/mmap/plo/Player;>; / Icon InnerClasses I(Lhaven/OwnerContext;Lhaven/Resource;Lhaven/Message;)Lhaven/GobIcon$Icon; 
SourceFile Factory.java   !haven/res/gfx/hud/mmap/plo/Player  0 java/util/ArrayList 1 2 3  4 - 5 6   "haven/res/gfx/hud/mmap/plo/Factory java/lang/Object haven/GobIcon$Icon$Factory Factory java/util/Collection 7 haven/GobIcon$Icon '(Lhaven/OwnerContext;Lhaven/Resource;)V haven/BuddyWnd gc [Ljava/awt/Color; ((Lhaven/OwnerContext;Lhaven/Resource;I)V add (Ljava/lang/Object;)Z haven/GobIcon 	plo.cjava ! 
                   *� �           M        "     
� Y+,� �           O        k     1� Y� :6� �� � Y+,� �  W�����        �  � !        S 	 T  U ( T . V     A             *+,-� 	�           M      8       . 	   ,	codeentry ?   mapicon haven.res.gfx.hud.mmap.plo.Factory   ui/obj/buddy   