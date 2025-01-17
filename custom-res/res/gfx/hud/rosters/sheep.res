Haven Resource 1= src y	  Sheep.java /* Preprocessed source code */
/* $use: ui/croster */

package haven.res.gfx.hud.rosters.sheep;

import haven.*;
import haven.res.ui.croster.*;
import java.util.*;

public class Sheep extends Entry {
    public int meat, milk, wool;
    public int meatq, milkq, woolq, hideq;
    public int seedq;
    public boolean ram, lamb, dead, pregnant, lactate, owned, mine;

    public Sheep(long id, String name) {
	super(SIZE, id, name);
    }

    public void draw(GOut g) {
	drawbg(g);
	int i = 0;
	drawcol(g, SheepRoster.cols.get(i), 0, this, namerend, i++);
	drawcol(g, SheepRoster.cols.get(i), 0.5, ram,      sex, i++);
	drawcol(g, SheepRoster.cols.get(i), 0.5, lamb,     growth, i++);
	drawcol(g, SheepRoster.cols.get(i), 0.5, dead,     deadrend, i++);
	drawcol(g, SheepRoster.cols.get(i), 0.5, pregnant, pregrend, i++);
	drawcol(g, SheepRoster.cols.get(i), 0.5, lactate,  lactrend, i++);
	drawcol(g, SheepRoster.cols.get(i), 0.5, (owned ? 1 : 0) | (mine ? 2 : 0), ownrend, i++);
	drawcol(g, SheepRoster.cols.get(i), 1, q, quality, i++);
	drawcol(g, SheepRoster.cols.get(i), 1, meat, null, i++);
	drawcol(g, SheepRoster.cols.get(i), 1, milk, null, i++);
	drawcol(g, SheepRoster.cols.get(i), 1, wool, null, i++);
	drawcol(g, SheepRoster.cols.get(i), 1, meatq, percent, i++);
	drawcol(g, SheepRoster.cols.get(i), 1, milkq, percent, i++);
	drawcol(g, SheepRoster.cols.get(i), 1, woolq, percent, i++);
	drawcol(g, SheepRoster.cols.get(i), 1, hideq, percent, i++);
	drawcol(g, SheepRoster.cols.get(i), 1, seedq, null, i++);
	super.draw(g);
    }

    public boolean mousedown(Coord c, int button) {
	if(SheepRoster.cols.get(1).hasx(c.x)) {
	    markall(Sheep.class, o -> (o.ram == this.ram));
	    return(true);
	}
	if(SheepRoster.cols.get(2).hasx(c.x)) {
	    markall(Sheep.class, o -> (o.lamb == this.lamb));
	    return(true);
	}
	if(SheepRoster.cols.get(3).hasx(c.x)) {
	    markall(Sheep.class, o -> (o.dead == this.dead));
	    return(true);
	}
	if(SheepRoster.cols.get(4).hasx(c.x)) {
	    markall(Sheep.class, o -> (o.pregnant == this.pregnant));
	    return(true);
	}
	if(SheepRoster.cols.get(5).hasx(c.x)) {
	    markall(Sheep.class, o -> (o.lactate == this.lactate));
	    return(true);
	}
	if(SheepRoster.cols.get(6).hasx(c.x)) {
	    markall(Sheep.class, o -> ((o.owned == this.owned) && (o.mine == this.mine)));
	    return(true);
	}
	return(super.mousedown(c, button));
    }
}

/* >wdg: SheepRoster */
src F  SheepRoster.java /* Preprocessed source code */
/* $use: ui/croster */

package haven.res.gfx.hud.rosters.sheep;

import haven.*;
import haven.res.ui.croster.*;
import java.util.*;

public class SheepRoster extends CattleRoster<Sheep> {
    public static List<Column> cols = initcols(
	new Column<Entry>("Name", Comparator.comparing((Entry e) -> e.name), 200),

	new Column<Sheep>(Resource.classres(SheepRoster.class).pool.load("gfx/hud/rosters/sex", 2),      Comparator.comparing((Sheep e) -> e.ram).reversed(), 20).runon(),
	new Column<Sheep>(Resource.classres(SheepRoster.class).pool.load("gfx/hud/rosters/growth", 2),   Comparator.comparing((Sheep e) -> e.lamb).reversed(), 20).runon(),
	new Column<Sheep>(Resource.classres(SheepRoster.class).pool.load("gfx/hud/rosters/deadp", 3),    Comparator.comparing((Sheep e) -> e.dead).reversed(), 20).runon(),
	new Column<Sheep>(Resource.classres(SheepRoster.class).pool.load("gfx/hud/rosters/pregnant", 2), Comparator.comparing((Sheep e) -> e.pregnant).reversed(), 20).runon(),
	new Column<Sheep>(Resource.classres(SheepRoster.class).pool.load("gfx/hud/rosters/lactate", 1),  Comparator.comparing((Sheep e) -> e.lactate).reversed(), 20),
	new Column<Sheep>(Resource.classres(SheepRoster.class).pool.load("gfx/hud/rosters/owned", 1),    Comparator.comparing((Sheep e) -> ((e.owned ? 1 : 0) | (e.mine ? 2 : 0))).reversed(), 20),

	new Column<Sheep>(Resource.classres(SheepRoster.class).pool.load("gfx/hud/rosters/quality", 2), Comparator.comparing((Sheep e) -> e.q).reversed()),

	new Column<Sheep>(Resource.classres(SheepRoster.class).pool.load("gfx/hud/rosters/meatquantity", 1), Comparator.comparing((Sheep e) -> e.meat).reversed()),
	new Column<Sheep>(Resource.classres(SheepRoster.class).pool.load("gfx/hud/rosters/milkquantity", 1), Comparator.comparing((Sheep e) -> e.milk).reversed()),
	new Column<Sheep>(Resource.classres(SheepRoster.class).pool.load("gfx/hud/rosters/woolquantity", 1), Comparator.comparing((Sheep e) -> e.wool).reversed()),

	new Column<Sheep>(Resource.classres(SheepRoster.class).pool.load("gfx/hud/rosters/meatquality", 1), Comparator.comparing((Sheep e) -> e.meatq).reversed()),
	new Column<Sheep>(Resource.classres(SheepRoster.class).pool.load("gfx/hud/rosters/milkquality", 1), Comparator.comparing((Sheep e) -> e.milkq).reversed()),
	new Column<Sheep>(Resource.classres(SheepRoster.class).pool.load("gfx/hud/rosters/woolquality", 1), Comparator.comparing((Sheep e) -> e.woolq).reversed()),
	new Column<Sheep>(Resource.classres(SheepRoster.class).pool.load("gfx/hud/rosters/hidequality", 1), Comparator.comparing((Sheep e) -> e.hideq).reversed()),

	new Column<Sheep>(Resource.classres(SheepRoster.class).pool.load("gfx/hud/rosters/breedingquality", 1), Comparator.comparing((Sheep e) -> e.seedq).reversed())
    );
    protected List<Column> cols() {return(cols);}

    public static CattleRoster mkwidget(UI ui, Object... args) {
	return(new SheepRoster());
    }

    public Sheep parse(Object... args) {
	int n = 0;
	long id = (Long)args[n++];
	String name = (String)args[n++];
	Sheep ret = new Sheep(id, name);
	ret.grp = (Integer)args[n++];
	int fl = (Integer)args[n++];
	ret.ram = (fl & 1) != 0;
	ret.lamb = (fl & 2) != 0;
	ret.dead = (fl & 4) != 0;
	ret.pregnant = (fl & 8) != 0;
	ret.lactate = (fl & 16) != 0;
	ret.owned = (fl & 32) != 0;
	ret.mine = (fl & 64) != 0;
	ret.q = ((Number)args[n++]).doubleValue();
	ret.meat = (Integer)args[n++];
	ret.milk = (Integer)args[n++];
	ret.wool = (Integer)args[n++];
	ret.meatq = (Integer)args[n++];
	ret.milkq = (Integer)args[n++];
	ret.woolq = (Integer)args[n++];
	ret.hideq = (Integer)args[n++];
	ret.seedq = (Integer)args[n++];
	return(ret);
    }

    public TypeButton button() {
	return(typebtn(Resource.classres(SheepRoster.class).pool.load("gfx/hud/rosters/btn-sheep", 2),
		       Resource.classres(SheepRoster.class).pool.load("gfx/hud/rosters/btn-sheep-d", 2)));
    }
}
code 7  haven.res.gfx.hud.rosters.sheep.Sheep ����   4 �	 ) Y
 2 Z
 ) [	 \ ] ^ _ `	 ) a
 ) b?�      	 ) c
 d e	 ) f	 ) g	 ) h	 ) i	 ) j	 ) k	 ) l	 ) m	 ) n	 ) o	 ) p
 q r	 ) s	 ) t
 u v	 ) w	 ) x	 ) y	 ) z	 ) {	 ) |	 ) }	 ) ~	 ) 	 ) �
 2 �	 � �
  � �   �
 ) �  �  �  �  �  �
 2 � � meat I milk wool meatq milkq woolq hideq seedq ram Z lamb dead pregnant lactate owned mine <init> (JLjava/lang/String;)V Code LineNumberTable draw (Lhaven/GOut;)V StackMapTable � � ` 	mousedown (Lhaven/Coord;I)Z lambda$mousedown$5 *(Lhaven/res/gfx/hud/rosters/sheep/Sheep;)Z lambda$mousedown$4 lambda$mousedown$3 lambda$mousedown$2 lambda$mousedown$1 lambda$mousedown$0 
SourceFile 
Sheep.java � � D � � I � � � � � � haven/res/ui/croster/Column � � � � < = � � � � � > = � � ? = � � @ = � � A = � � B = C = � � � � � � � � � � � � 3 4 5 4 6 4 7 4 � � 8 4 9 4 : 4 ; 4 H I � � 4 � � %haven/res/gfx/hud/rosters/sheep/Sheep BootstrapMethods � � � Q � � � � � � � � � N O haven/res/ui/croster/Entry 
haven/GOut SIZE Lhaven/Coord; #(Lhaven/Coord;JLjava/lang/String;)V drawbg +haven/res/gfx/hud/rosters/sheep/SheepRoster cols Ljava/util/List; java/util/List get (I)Ljava/lang/Object; namerend Ljava/util/function/Function; drawcol ](Lhaven/GOut;Lhaven/res/ui/croster/Column;DLjava/lang/Object;Ljava/util/function/Function;I)V java/lang/Boolean valueOf (Z)Ljava/lang/Boolean; sex growth deadrend pregrend lactrend java/lang/Integer (I)Ljava/lang/Integer; ownrend q D java/lang/Double (D)Ljava/lang/Double; quality percent haven/Coord x hasx (I)Z
 � � (Ljava/lang/Object;)Z
 ) � test G(Lhaven/res/gfx/hud/rosters/sheep/Sheep;)Ljava/util/function/Predicate; markall 2(Ljava/lang/Class;Ljava/util/function/Predicate;)V
 ) �
 ) �
 ) �
 ) �
 ) � � � � V Q U Q T Q S Q R Q P Q "java/lang/invoke/LambdaMetafactory metafactory � Lookup InnerClasses �(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite; � %java/lang/invoke/MethodHandles$Lookup java/lang/invoke/MethodHandles sheep.cjava ! ) 2     3 4    5 4    6 4    7 4    8 4    9 4    : 4    ; 4    < =    > =    ? =    @ =    A =    B =    C =   	  D E  F   &     
*� -� �    G   
     	   H I  F  �     *+� =*+� �  � *� �� *+� �  �  	*� � � �� *+� �  �  	*� � � �� *+� �  �  	*� � � �� *+� �  �  	*� � � �� *+� �  �  	*� � � �� *+� �  �  	*� � � *� � � �� � �� *+� �  � *� � � �� *+� �  � *� � �� *+� �  � *� � �� *+� �  � *� � �� *+� �  � *�  � � !�� *+� �  � *� "� � !�� *+� �  � *� #� � !�� *+� �  � *� $� � !�� *+� �  � *� %� �� *+� &�    J   f � �  K L  K L M�    K L  K L M� 
  K L  K L M�    K L  K L M G   R         !  C  e  �  �  �  " @ ^  | !� "� #� $� % & '  N O  F  N     � �  � +� '� (� *)*� *  � +�� �  � +� '� (� *)*� ,  � +�� �  � +� '� (� *)*� -  � +�� �  � +� '� (� *)*� .  � +�� �  � +� '� (� *)*� /  � +�� �  � +� '� (� *)*� 0  � +�*+� 1�    J    $####$ G   N    *  + " , $ . : / F 0 H 2 ^ 3 j 4 l 6 � 7 � 8 � : � ; � < � > � ? � @ � B P Q  F   ?     +� *� � +� *� � � �    J    @ G       ? R Q  F   4     +� *� � � �    J    @ G       ; S Q  F   4     +� *� � � �    J    @ G       7 T Q  F   4     +� *� � � �    J    @ G       3 U Q  F   4     +� *� � � �    J    @ G       / V Q  F   4     +� *� � � �    J    @ G       +  �   >  �  � � � �  � � � �  � � � �  � � � �  � � � �  � � � W    � �   
  � � � code b  haven.res.gfx.hud.rosters.sheep.SheepRoster ����   4O
 R �	  � �
  � �
  � � �
  � �
 
 �	  �	  �	  �	  �	  �	  �	  �	  � �
  �	  �	  �	  �	  �	  �	  �	  �	  �	  �
 � �	 � � �
 � � �
  �
  �
 
 �
 � �
 � �	 � � � �   � � �
 * � �  � � �
 * �
 * � �  � �  � �  � �  � �  � �  �
 * � �  � � 	 � � 
 � �  � �  � �  � �  � �  �
  � � cols Ljava/util/List; 	Signature /Ljava/util/List<Lhaven/res/ui/croster/Column;>; <init> ()V Code LineNumberTable ()Ljava/util/List; 1()Ljava/util/List<Lhaven/res/ui/croster/Column;>; mkwidget B(Lhaven/UI;[Ljava/lang/Object;)Lhaven/res/ui/croster/CattleRoster; parse <([Ljava/lang/Object;)Lhaven/res/gfx/hud/rosters/sheep/Sheep; StackMapTable � � � � button #()Lhaven/res/ui/croster/TypeButton; 1([Ljava/lang/Object;)Lhaven/res/ui/croster/Entry; lambda$static$15 <(Lhaven/res/gfx/hud/rosters/sheep/Sheep;)Ljava/lang/Integer; lambda$static$14 lambda$static$13 lambda$static$12 lambda$static$11 lambda$static$10 lambda$static$9 lambda$static$8 lambda$static$7 ;(Lhaven/res/gfx/hud/rosters/sheep/Sheep;)Ljava/lang/Double; lambda$static$6 lambda$static$5 <(Lhaven/res/gfx/hud/rosters/sheep/Sheep;)Ljava/lang/Boolean; lambda$static$4 lambda$static$3 lambda$static$2 lambda$static$1 lambda$static$0 0(Lhaven/res/ui/croster/Entry;)Ljava/lang/String; <clinit> LLhaven/res/ui/croster/CattleRoster<Lhaven/res/gfx/hud/rosters/sheep/Sheep;>; 
SourceFile SheepRoster.java W X S T +haven/res/gfx/hud/rosters/sheep/SheepRoster java/lang/Long � � java/lang/String %haven/res/gfx/hud/rosters/sheep/Sheep W � java/lang/Integer � � � � � � � � � � � � � � � � � � java/lang/Number � � � � � � � � � � � � � � � � � � � � � � � � gfx/hud/rosters/btn-sheep gfx/hud/rosters/btn-sheep-d	 _ `


 haven/res/ui/croster/Column Name BootstrapMethods | W gfx/hud/rosters/sex v W ! gfx/hud/rosters/growth" gfx/hud/rosters/deadp# gfx/hud/rosters/pregnant$ gfx/hud/rosters/lactate% gfx/hud/rosters/owned& j gfx/hud/rosters/quality' s W( gfx/hud/rosters/meatquantity) gfx/hud/rosters/milkquantity* gfx/hud/rosters/woolquantity+ gfx/hud/rosters/meatquality, gfx/hud/rosters/milkquality- gfx/hud/rosters/woolquality. gfx/hud/rosters/hidequality/ gfx/hud/rosters/breedingquality012 !haven/res/ui/croster/CattleRoster [Ljava/lang/Object; 	longValue ()J (JLjava/lang/String;)V intValue ()I grp I ram Z lamb dead pregnant lactate owned mine doubleValue ()D q D meat milk wool meatq milkq woolq hideq seedq haven/Resource classres #(Ljava/lang/Class;)Lhaven/Resource; pool Pool InnerClasses Lhaven/Resource$Pool; haven/Resource$Pool load3 Named +(Ljava/lang/String;I)Lhaven/Resource$Named; typebtn =(Lhaven/Indir;Lhaven/Indir;)Lhaven/res/ui/croster/TypeButton; valueOf (I)Ljava/lang/Integer; java/lang/Double (D)Ljava/lang/Double; java/lang/Boolean (Z)Ljava/lang/Boolean; haven/res/ui/croster/Entry name Ljava/lang/String;
45 &(Ljava/lang/Object;)Ljava/lang/Object;
 6 apply ()Ljava/util/function/Function; java/util/Comparator 	comparing 5(Ljava/util/function/Function;)Ljava/util/Comparator; ,(Ljava/lang/String;Ljava/util/Comparator;I)V
 7 reversed ()Ljava/util/Comparator; '(Lhaven/Indir;Ljava/util/Comparator;I)V runon ()Lhaven/res/ui/croster/Column;
 8
 9
 :
 ;
 <
 = &(Lhaven/Indir;Ljava/util/Comparator;)V
 >
 ?
 @
 A
 B
 C
 D
 E initcols 0([Lhaven/res/ui/croster/Column;)Ljava/util/List; haven/Resource$NamedFGJ { | z v y v x v w v u v t j r s q j p j o j n j m j l j k j i j "java/lang/invoke/LambdaMetafactory metafactoryL Lookup �(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;M %java/lang/invoke/MethodHandles$Lookup java/lang/invoke/MethodHandles sheep.cjava !  R    	 S T  U    V   W X  Y        *� �    Z       G  S [  Y        � �    Z       _ U    \ � ] ^  Y         � Y� �    Z       b � _ `  Y  �    \=+�2� � B+�2� :� Y!� 	:+�2� 
� � +�2� 
� 6~� � � ~� � � ~� � � ~� � � ~� � �  ~� � � @~� � � +�2� � � +�2� 
� � +�2� 
� � +�2� 
� � +�2� 
� � +�2� 
� � +�2� 
� � +�2� 
� � +�2� 
� � �    a   � � R  b c d e  e�    b c d e  eO e�    b c d e  eO e�    b c d e  eP e�    b c d e  eP e�    b c d e  eP e�    b c d e  eP e�    b c d e  e Z   ^    f  g  h  i & j 7 k E l V m g n x o � p � q � r � s � t � u � v w x& y7 zH {Y |  f g  Y   @      � �  !� "� �  #� "� $�    Z       �  �  �A _ h  Y        *+� %�    Z       G
 i j  Y         *� � &�    Z       ]
 k j  Y         *� � &�    Z       [
 l j  Y         *� � &�    Z       Z
 m j  Y         *� � &�    Z       Y
 n j  Y         *� � &�    Z       X
 o j  Y         *� � &�    Z       V
 p j  Y         *� � &�    Z       U
 q j  Y         *� � &�    Z       T
 r s  Y         *� � '�    Z       R
 t j  Y   N     *� � � *� � � �� &�    a    @J�    e  Z       P
 u v  Y         *� � (�    Z       O
 w v  Y         *� � (�    Z       N
 x v  Y         *� � (�    Z       M
 y v  Y         *� � (�    Z       L
 z v  Y         *� � (�    Z       K
 { |  Y        *� )�    Z       I  } X  Y  �     p� *Y� *Y+� ,  � - ȷ .SY� *Y� �  /� "� 0  � -� 1 � 2� 3SY� *Y� �  4� "� 5  � -� 1 � 2� 3SY� *Y� �  6� "� 7  � -� 1 � 2� 3SY� *Y� �  8� "� 9  � -� 1 � 2� 3SY� *Y� �  :� "� ;  � -� 1 � 2SY� *Y� �  <� "� =  � -� 1 � 2SY� *Y� �  >� "� ?  � -� 1 � @SY� *Y� �  A� "� B  � -� 1 � @SY	� *Y� �  C� "� D  � -� 1 � @SY
� *Y� �  E� "� F  � -� 1 � @SY� *Y� �  G� "� H  � -� 1 � @SY� *Y� �  I� "� J  � -� 1 � @SY� *Y� �  K� "� L  � -� 1 � @SY� *Y� �  M� "� N  � -� 1 � @SY� *Y� �  O� "� P  � -� 1 � @S� Q� �    Z   J    H  I $ K N L x M � N � O � P RB Th U� V� X� Y  Z& [L ]i H  �   �  �  � � � �  � � � �  � � � �  � � � �  � � � �  � � � �  � � � �  � � � �  � � � �  � � � �  � � � �  � � � �  � � � �  � � � �  � � � �  � � �    N U    ~     � �  	 �	HKI codeentry B   wdg haven.res.gfx.hud.rosters.sheep.SheepRoster   ui/croster H  