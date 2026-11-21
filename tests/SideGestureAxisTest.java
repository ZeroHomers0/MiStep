package com.jiusis.mistep;

public final class SideGestureAxisTest {
    public static void main(String[] args) {
        int checks=0;
        for (boolean left : new boolean[]{false,true}) {
            for (float sign : new float[]{-1,1}) {
                if (MiStepSideGestureAxis.resolve(left,10*sign,10,8)!=0)throw new AssertionError("diagonal claimed");checks++;
                if (MiStepSideGestureAxis.resolve(left,10,30*sign,8)!=1)throw new AssertionError("vertical not scrolling");checks++;
                int expected=(left?-sign:sign)>0?2:3;
                if (MiStepSideGestureAxis.resolve(left,30*sign,10,8)!=expected)throw new AssertionError("wrong horizontal direction");checks++;
            }
        }
        for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}) {
            if(MiStepSideGestureAxis.resolve(true,bad,30,8)!=0)throw new AssertionError("invalid coordinate");checks++;
        }
        if(MiStepSideGestureAxis.resolve(true,-7,0,8)!=0)throw new AssertionError("jitter");checks++;
        System.out.println("Side gesture arbitration: "+checks+" checks passed");
    }
}
