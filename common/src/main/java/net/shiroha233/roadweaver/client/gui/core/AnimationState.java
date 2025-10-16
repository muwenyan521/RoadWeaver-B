package net.shiroha233.roadweaver.client.gui.core;

/**
 * 动画状态管理器
 * <p>
 * 提供常用的动画效果和缓动函数。
 * 支持淡入淡出、弹性动画、脉冲效果等。
 * </p>
 * 
 * @author RoadWeaver Team
 * @version 2.0
 */
public class AnimationState {
    
    private float value;
    private float target;
    private float speed;
    private EasingFunction easing;
    
    /**
     * 构造动画状态
     * @param initialValue 初始值
     * @param speed 动画速度（0-1，越大越快）
     */
    public AnimationState(float initialValue, float speed) {
        this.value = initialValue;
        this.target = initialValue;
        this.speed = Math.max(0.01f, Math.min(1.0f, speed));
        this.easing = EasingFunction.EASE_OUT_CUBIC;
    }
    
    /**
     * 更新动画
     * @param deltaTime 时间增量
     */
    public void update(float deltaTime) {
        if (Math.abs(value - target) < 0.001f) {
            value = target;
            return;
        }
        
        float diff = target - value;
        float step = diff * speed * deltaTime * 60.0f; // 归一化到60FPS
        value += easing.apply(Math.abs(step / diff)) * diff * speed;
    }
    
    /**
     * 设置目标值
     */
    public void setTarget(float target) {
        this.target = target;
    }
    
    /**
     * 立即设置值
     */
    public void setValue(float value) {
        this.value = value;
        this.target = value;
    }
    
    /**
     * 设置缓动函数
     */
    public void setEasing(EasingFunction easing) {
        this.easing = easing;
    }
    
    /**
     * 获取当前值
     */
    public float getValue() {
        return value;
    }
    
    /**
     * 是否正在动画
     */
    public boolean isAnimating() {
        return Math.abs(value - target) > 0.001f;
    }
    
    /**
     * 缓动函数接口
     */
    @FunctionalInterface
    public interface EasingFunction {
        float apply(float t);
        
        // 预定义缓动函数
        EasingFunction LINEAR = t -> t;
        EasingFunction EASE_IN_QUAD = t -> t * t;
        EasingFunction EASE_OUT_QUAD = t -> t * (2 - t);
        EasingFunction EASE_IN_OUT_QUAD = t -> t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
        EasingFunction EASE_IN_CUBIC = t -> t * t * t;
        EasingFunction EASE_OUT_CUBIC = t -> {
            float f = t - 1;
            return f * f * f + 1;
        };
        EasingFunction EASE_IN_OUT_CUBIC = t -> {
            if (t < 0.5f) return 4 * t * t * t;
            float f = (2 * t - 2);
            return 0.5f * f * f * f + 1;
        };
        EasingFunction ELASTIC = t -> {
            if (t == 0 || t == 1) return t;
            return (float)(Math.pow(2, -10 * t) * Math.sin((t - 0.075) * (2 * Math.PI) / 0.3) + 1);
        };
    }
    
    /**
     * 脉冲动画
     */
    public static class PulseAnimation {
        private float phase = 0;
        private final float speed;
        private final float min;
        private final float max;
        
        public PulseAnimation(float min, float max, float speed) {
            this.min = min;
            this.max = max;
            this.speed = speed;
        }
        
        public void update(float deltaTime) {
            phase += speed * deltaTime;
            if (phase > Math.PI * 2) {
                phase -= Math.PI * 2;
            }
        }
        
        public float getValue() {
            return min + (max - min) * (float)(Math.sin(phase) * 0.5 + 0.5);
        }
    }
    
    /**
     * 淡入淡出动画
     */
    public static class FadeAnimation extends AnimationState {
        private boolean visible;
        
        public FadeAnimation(float speed) {
            super(0.0f, speed);
            this.visible = false;
        }
        
        public void show() {
            visible = true;
            setTarget(1.0f);
        }
        
        public void hide() {
            visible = false;
            setTarget(0.0f);
        }
        
        public void toggle() {
            if (visible) hide();
            else show();
        }
        
        public boolean isVisible() {
            return getValue() > 0.01f;
        }
        
        public boolean isFullyVisible() {
            return getValue() > 0.99f;
        }
    }
}
