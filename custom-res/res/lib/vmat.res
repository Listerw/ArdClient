Haven Resource 1$ src �  Mapping.java /* Preprocessed source code */
package haven.res.lib.vmat;

import haven.*;
import haven.render.*;
import haven.resutil.OverTex;
import java.util.*;
import java.util.function.Consumer;

public abstract class Mapping extends GAttrib {
    public abstract Material mergemat(Material orig, int mid);

    public Mapping(Gob gob) {
	super(gob);
    }

    public RenderTree.Node[] apply(Resource res) {
	Collection<RenderTree.Node> rl = new LinkedList<RenderTree.Node>();
	for(FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
	    String sid = mr.rdat.get("vm");
	    int mid = (sid == null)?-1:Integer.parseInt(sid);
	    if(mid >= 0) {
		rl.add(mergemat(mr.mat.get(), mid).apply(mr.m));
	    } else if(mr.mat != null) {
		rl.add(mr.mat.get().apply(mr.m));
	    }
	}
	return(rl.toArray(new RenderTree.Node[0]));
    }

    public final static Mapping empty = new Mapping(null) {
	    public Material mergemat(Material orig, int mid) {
		return(orig);
	    }
	};
}

/* >objdelta: Materials */
src   Materials.java /* Preprocessed source code */
package haven.res.lib.vmat;

import haven.*;
import haven.render.*;
import haven.resutil.OverTex;
import java.util.*;
import java.util.function.Consumer;

public class Materials extends Mapping {
    public static final Map<Integer, Material> empty = Collections.<Integer, Material>emptyMap();
    public final Map<Integer, Material> mats;

    public static Map<Integer, Material> decode(Resource.Resolver rr, Message sdt) {
	Map<Integer, Material> ret = new IntMap<Material>();
	int idx = 0;
	while(!sdt.eom()) {
	    Indir<Resource> mres = rr.getres(sdt.uint16());
	    int mid = sdt.int8();
	    Material.Res mat;
	    if(mid >= 0)
		mat = mres.get().layer(Material.Res.class, mid);
	    else
		mat = mres.get().layer(Material.Res.class);
	    ret.put(idx++, mat.get());
	}
	return(ret);
    }

    private static final Collection<Pair<TexRender.TexDraw, TexRender.TexClip>> warned = new HashSet<>();
    public static Material stdmerge(Material orig, Material var) {
	return(new Material(Pipe.Op.compose(orig.states, var.states),
			    Pipe.Op.compose(orig.dynstates, var.dynstates)));
    }

    public Material mergemat(Material orig, int mid) {
	if(!mats.containsKey(mid))
	    return(orig);
	Material var = mats.get(mid);
	return(stdmerge(orig, var));
    }

    public Materials(Gob gob, Map<Integer, Material> mats) {
	super(gob);
	this.mats = mats;
    }

    public static void parse(Gob gob, Message dat) {
	gob.setattr(new Materials(gob, decode(gob.context(Resource.Resolver.class), dat)));
    }
}

src �  Wrapping.java /* Preprocessed source code */
package haven.res.lib.vmat;

import haven.*;
import haven.render.*;
import haven.resutil.OverTex;
import java.util.*;
import java.util.function.Consumer;

public class Wrapping extends Pipe.Op.Wrapping {
    public final int mid;

    public Wrapping(RenderTree.Node r, Pipe.Op st, int mid) {
	super(r, st, true);
	this.mid = mid;
    }

    public String toString() {
	return(String.format("#<vmat %s %s>", mid, op));
    }
}

/* >spr: VarSprite */
src {  VarSprite.java /* Preprocessed source code */
package haven.res.lib.vmat;

import haven.*;
import haven.render.*;
import haven.resutil.OverTex;
import java.util.*;
import java.util.function.Consumer;

public class VarSprite extends SkelSprite {
    private Mapping cmats;

    public VarSprite(Owner owner, Resource res, Message sdt) {
	super(owner, res, sdt);
    }

    public Mapping mats() {
	return(Optional.ofNullable((owner instanceof Gob) ? ((Gob)owner) : null).map(gob -> gob.getattr(Mapping.class)).orElse(Mapping.empty));
    }

    public void iparts(int mask, Collection<RenderTree.Node> rbuf, Collection<Runnable> tbuf, Collection<Consumer<Render>> gbuf) {
	Mapping mats = (this.cmats == null) ? Mapping.empty : cmats;
	for(FastMesh.MeshRes mr : res.layers(FastMesh.MeshRes.class)) {
	    String sid = mr.rdat.get("vm");
	    int mid = (sid == null) ? -1 : Integer.parseInt(sid);
	    if(((mr.mat != null) || (mid >= 0)) && ((mr.id < 0) || (((1 << mr.id) & mask) != 0)))
		rbuf.add(animwrap(new Wrapping(mr.m, mats.mergemat(mr.mat.get(), mid), mid), tbuf, gbuf));
	}
	Owner rec = null;
	for(RenderLink.Res lr : res.layers(RenderLink.Res.class)) {
	    if((lr.id < 0) || (((1 << lr.id) & mask) != 0)) {
		if(rec == null)
		    rec = new RecOwner();
		RenderTree.Node r = lr.l.make(rec);
		if(r instanceof Pipe.Op.Wrapping)
		    r = animwrap((Pipe.Op.Wrapping)r, tbuf, gbuf);
		rbuf.add(r);
	    }
	}
    }

    public boolean tick(double dt) {
	Mapping mats = mats(), pmats = this.cmats;
	if(mats != pmats) {
	    try {
		this.cmats = mats;
		update();
	    } catch(Loading l) {
		this.cmats = pmats;
	    }
	}
	return(super.tick(dt));
    }
}
code �  haven.res.lib.vmat.Mapping$1 ����   4 
     <init> (Lhaven/Gob;)V Code LineNumberTable mergemat #(Lhaven/Material;I)Lhaven/Material; 
SourceFile Mapping.java EnclosingMethod   haven/res/lib/vmat/Mapping$1 InnerClasses haven/res/lib/vmat/Mapping 
vmat.cjava 0                     *+� �              	          +�              
        
              code �  haven.res.lib.vmat.Mapping ����   4 �
  1 2
  3 5
 7 8 9 : ; < ; =	  > ? @ A B
 C D	  E
 F G
  H	  I
 J K 9 L N 9 O P Q
  1	  R S T InnerClasses empty Lhaven/res/lib/vmat/Mapping; mergemat #(Lhaven/Material;I)Lhaven/Material; <init> (Lhaven/Gob;)V Code LineNumberTable apply Node 1(Lhaven/Resource;)[Lhaven/render/RenderTree$Node; StackMapTable U V 5 B <clinit> ()V 
SourceFile Mapping.java ! " java/util/LinkedList ! . W haven/FastMesh$MeshRes MeshRes X Y Z U [ \ V ] ^ _ ` a b vm c d e java/lang/String f g h i k l d m    n o p % u v w x haven/render/RenderTree$Node y z [Lhaven/render/RenderTree$Node; haven/res/lib/vmat/Mapping$1   haven/res/lib/vmat/Mapping haven/GAttrib java/util/Collection java/util/Iterator haven/FastMesh haven/Resource layers )(Ljava/lang/Class;)Ljava/util/Collection; iterator ()Ljava/util/Iterator; hasNext ()Z next ()Ljava/lang/Object; rdat Ljava/util/Map; java/util/Map get &(Ljava/lang/Object;)Ljava/lang/Object; java/lang/Integer parseInt (Ljava/lang/String;)I mat Res Lhaven/Material$Res; haven/Material$Res ()Lhaven/Material; m Lhaven/FastMesh; haven/Material | Op } Wrapping ?(Lhaven/render/RenderTree$Node;)Lhaven/render/Pipe$Op$Wrapping; add (Ljava/lang/Object;)Z haven/render/RenderTree toArray (([Ljava/lang/Object;)[Ljava/lang/Object; ~ haven/render/Pipe$Op haven/render/Pipe$Op$Wrapping haven/render/Pipe 
vmat.cjava!                  ! "  #   "     *+� �    $   
        % '  #        �� Y� M+� �  N-�  � v-�  � :� 	
�  � :� � � 6� #,*� � � � � �  W� "� � ,� � � � �  W���,� �  � �    (     �  ) *� - + ,D� &� �  $   * 
      (  9  I  N  n  v  �  �   - .  #   $      � Y� � �    $         /        2         M &	  4 6 	 F J j 	 q { r	 s q t 	code   haven.res.lib.vmat.Materials ����   4 � J
  K
 L M
 L N  O
 L P Q R S T
 V W
  X
  Y
 	 Z [ \ ] _	  a  b	  c
  d	  e [ f [ g
  h
 % i j k
 l m
  n
  o
 l p
 q r	  s t
 " K	  u v empty Ljava/util/Map; 	Signature 4Ljava/util/Map<Ljava/lang/Integer;Lhaven/Material;>; mats warned Ljava/util/Collection; x TexDraw InnerClasses y TexClip XLjava/util/Collection<Lhaven/Pair<Lhaven/TexRender$TexDraw;Lhaven/TexRender$TexClip;>;>; decode Resolver 9(Lhaven/Resource$Resolver;Lhaven/Message;)Ljava/util/Map; Code LineNumberTable StackMapTable z { T ^(Lhaven/Resource$Resolver;Lhaven/Message;)Ljava/util/Map<Ljava/lang/Integer;Lhaven/Material;>; stdmerge 2(Lhaven/Material;Lhaven/Material;)Lhaven/Material; mergemat #(Lhaven/Material;I)Lhaven/Material; <init> (Lhaven/Gob;Ljava/util/Map;)V B(Lhaven/Gob;Ljava/util/Map<Ljava/lang/Integer;Lhaven/Material;>;)V parse (Lhaven/Gob;Lhaven/Message;)V <clinit> ()V 
SourceFile Materials.java haven/IntMap A G | } ~  � � � � � { � � haven/Resource haven/Material$Res Res � � � � � � � � � z � � haven/Material � haven/render/Pipe$Op Op � � � � � � A � * ' � � � � = > A � haven/res/lib/vmat/Materials haven/Resource$Resolver � � � 3 5 A B � � � � � & ' java/util/HashSet + , haven/res/lib/vmat/Mapping � haven/TexRender$TexDraw haven/TexRender$TexClip java/util/Map haven/Indir haven/Message eom ()Z uint16 ()I getres (I)Lhaven/Indir; int8 get ()Ljava/lang/Object; java/lang/Integer valueOf (I)Ljava/lang/Integer; layer � IDLayer =(Ljava/lang/Class;Ljava/lang/Object;)Lhaven/Resource$IDLayer; � Layer )(Ljava/lang/Class;)Lhaven/Resource$Layer; ()Lhaven/Material; put 8(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object; haven/render/Pipe states Lhaven/render/Pipe$Op; compose /([Lhaven/render/Pipe$Op;)Lhaven/render/Pipe$Op; 	dynstates ([Lhaven/render/Pipe$Op;)V containsKey (Ljava/lang/Object;)Z &(Ljava/lang/Object;)Ljava/lang/Object; (Lhaven/Gob;)V 	haven/Gob context %(Ljava/lang/Class;)Ljava/lang/Object; setattr (Lhaven/GAttrib;)V java/util/Collections emptyMap ()Ljava/util/Map; haven/TexRender haven/Resource$IDLayer haven/Resource$Layer 
vmat.cjava !  %     & '  (    )  * '  (    )  + ,  (    2  	 3 5  6   �     p� Y� M>+� � `*+� �  :+� 6� �  � 	� 
� � 	:� �  � 	� � 	:,�� 
� �  W���,�    8    � 
 9� 9 :�  ;�  7   .    +  , 
 -  .  / # 1 ( 2 D 4 X 5 k 6 n 7 (    < 	 = >  6   \ 	    <� Y� Y� Y*� SY+� S� SY� Y*� SY+� S� S� �    7       < 4 = ; <  ? @  6   V     )*� � 
�  � +�*� � 
�  � N+-� �    8     7       A  B  C # D  A B  6   +     *+� *,� �    7       H  I 
 J (    C 	 D E  6   6     *� Y**� � +� � � �    7   
    M  N  F G  6   -      �  � !� "Y� #� $�    7   
    '  :  H    � /   :  - w . 	 0 w 1 	   4	 	  U 	  ^ `	 �  �	 �  �code �  haven.res.lib.vmat.Wrapping ����   4 5
 	 	    
  	  
   ! " # mid I <init> & Node InnerClasses ( Op 8(Lhaven/render/RenderTree$Node;Lhaven/render/Pipe$Op;I)V Code LineNumberTable toString ()Ljava/lang/String; 
SourceFile Wrapping.java  ) 
  #<vmat %s %s> java/lang/Object * + , - . / 0 1 haven/res/lib/vmat/Wrapping haven/render/Pipe$Op$Wrapping Wrapping 2 haven/render/RenderTree$Node 3 haven/render/Pipe$Op 8(Lhaven/render/RenderTree$Node;Lhaven/render/Pipe$Op;Z)V java/lang/Integer valueOf (I)Ljava/lang/Integer; op Lhaven/render/Pipe$Op; java/lang/String format 9(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String; haven/render/RenderTree haven/render/Pipe 
vmat.cjava !  	     
            -     *+,� *� �           U  V  W        3     � Y*� � SY*� S� �           Z      4       % 	  ' 	 	  $ 	code �  haven.res.lib.vmat.VarSprite ����   4 �
 , O	 + P Q
 R S   Y
 R Z	 	 [
 R \ ]	 + ^	 + _ a
 c d e f g h g i	  j k l m n
 o p	  q	  r s	  t
 u v
 	 w
  x
 + y e z |	  r 
 ! �	  � { � �
 + �
 + � �
 , �
  � � � cmats Lhaven/res/lib/vmat/Mapping; <init> � Owner InnerClasses 6(Lhaven/Sprite$Owner;Lhaven/Resource;Lhaven/Message;)V Code LineNumberTable mats ()Lhaven/res/lib/vmat/Mapping; StackMapTable Q iparts F(ILjava/util/Collection;Ljava/util/Collection;Ljava/util/Collection;)V ] � a n � | � 	Signature � Node �(ILjava/util/Collection<Lhaven/render/RenderTree$Node;>;Ljava/util/Collection<Ljava/lang/Runnable;>;Ljava/util/Collection<Ljava/util/function/Consumer<Lhaven/render/Render;>;>;)V tick (D)Z � � lambda$mats$0 )(Lhaven/Gob;)Lhaven/res/lib/vmat/Mapping; 
SourceFile VarSprite.java / 3 � � 	haven/Gob � � � BootstrapMethods � � � L � � � � � . � � haven/res/lib/vmat/Mapping - . � � � haven/FastMesh$MeshRes MeshRes � � � � � � � � � � � � � vm � � � java/lang/String � � � � � � � haven/res/lib/vmat/Wrapping � � � � � � � / � � � � � � haven/RenderLink$Res Res � haven/Sprite$RecOwner RecOwner / � � � � � � haven/render/Pipe$Op$Wrapping Op Wrapping 6 7 � � haven/Loading G H � � haven/res/lib/vmat/VarSprite haven/SkelSprite haven/Sprite$Owner java/util/Iterator haven/render/RenderTree$Node � owner Lhaven/Sprite$Owner; java/util/Optional 
ofNullable ((Ljava/lang/Object;)Ljava/util/Optional;
 � � &(Ljava/lang/Object;)Ljava/lang/Object;
 + � apply ()Ljava/util/function/Function; map 3(Ljava/util/function/Function;)Ljava/util/Optional; empty orElse res Lhaven/Resource; haven/FastMesh haven/Resource layers )(Ljava/lang/Class;)Ljava/util/Collection; java/util/Collection iterator ()Ljava/util/Iterator; hasNext ()Z next ()Ljava/lang/Object; rdat Ljava/util/Map; java/util/Map get java/lang/Integer parseInt (Ljava/lang/String;)I mat Lhaven/Material$Res; id I m Lhaven/FastMesh; � haven/Material$Res ()Lhaven/Material; mergemat #(Lhaven/Material;I)Lhaven/Material; 8(Lhaven/render/RenderTree$Node;Lhaven/render/Pipe$Op;I)V animwrap k(Lhaven/render/Pipe$Op$Wrapping;Ljava/util/Collection;Ljava/util/Collection;)Lhaven/render/RenderTree$Node; add (Ljava/lang/Object;)Z haven/RenderLink haven/Sprite (Lhaven/Sprite;)V l Lhaven/RenderLink; make 4(Lhaven/Sprite$Owner;)Lhaven/render/RenderTree$Node; � haven/render/Pipe$Op update ()V getattr "(Ljava/lang/Class;)Lhaven/GAttrib; haven/render/RenderTree � � � K L haven/Material haven/render/Pipe "java/lang/invoke/LambdaMetafactory metafactory � Lookup �(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; � %java/lang/invoke/MethodHandles$Lookup java/lang/invoke/MethodHandles 
vmat.cjava ! + ,     - .     / 3  4   $     *+,-� �    5   
    c  d  6 7  4   O     **� � � *� � � � �   � � � � 	�    8    @ 9 5       g  : ;  4  �  
  &*� 
� 	� � *� 
:*� � �  :�  � �  � :� �  � :� � � 6	� � 	� B� � � x~� .,*� Y� � � 	� 	� -� �  W��}:*� � �  :�  � b�  � :�  � �  x~� ?� � !Y*� ":� #� $ :		� %� *	� %-� :	,	�  W����    8   B C <�  < =� / > ?D� � *� �  @ =� ) A� # B� �  5   J    k  l 9 m J n Z o { p � q � r � s � t � u � v � w x y z" |% } C    F  G H  4   �     +*� &N*� 
:-� *-� 
*� '� :*� 
*'� )�     (  8    �   I < <  J 5   "    �  �  �  �  �  �  � % �
 K L  4   "     
*	� *� 	�    5       g  T     U  V W X M    � 2   J 	 0 ~ 1	 D � E	  ` b 	  { } 	 ! ~ �  � � �	 % � � 	 u � } 	 � � � codeentry J   objdelta haven.res.lib.vmat.Materials spr haven.res.lib.vmat.VarSprite   