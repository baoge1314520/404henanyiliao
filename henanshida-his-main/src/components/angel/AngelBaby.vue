<template>
	<svg
		class="angel-baby"
		:class="{ lively: animated }"
		:width="size"
		:height="size"
		:viewBox="viewBox"
		fill="none"
		xmlns="http://www.w3.org/2000/svg"
		aria-hidden="true"
		focusable="false"
	>
		<!-- 双翼（先画，垫在头后面） -->
		<g class="wing wing-l">
			<path d="M58,90 C46,82 42,74 46,66" />
			<path d="M58,100 C44,96 38,88 40,80" />
			<path d="M58,110 C46,110 40,104 40,96" />
		</g>
		<g class="wing wing-r">
			<path d="M142,90 C154,82 158,74 154,66" />
			<path d="M142,100 C156,96 162,88 160,80" />
			<path d="M142,110 C154,110 160,104 160,96" />
		</g>

		<!-- 天线 -->
		<path class="ab-line" d="M100,58 L100,46" />
		<rect class="ab-line" x="78" y="30" width="44" height="16" rx="8" />

		<!-- 双脚 -->
		<path class="ab-line" d="M84,140 v8 a7,7 0 0 0 14,0 v-8" />
		<path class="ab-line" d="M104,140 v8 a7,7 0 0 0 14,0 v-8" />

		<!-- 圆角方头 -->
		<rect class="ab-line ab-head" x="58" y="58" width="84" height="84" rx="22" />

		<!-- 脸 -->
		<circle class="ab-eye" cx="84" cy="98" r="6.5" />
		<circle class="ab-eye" cx="116" cy="98" r="6.5" />
		<path class="ab-line ab-smile" d="M92,110 Q100,118 108,110" />
		<path class="ab-blush" d="M74,110 L82,110" />
		<path class="ab-blush" d="M118,110 L126,110" />
	</svg>
</template>

<script setup>
defineProps({
	size: { type: [Number, String], default: 64 },
	animated: { type: Boolean, default: false },
	viewBox: { type: String, default: '0 0 200 200' }
})
</script>

<style scoped lang="scss">
.angel-baby {
	display: block;
	overflow: visible;
	color: var(--ab-lav, #a189c9);
}

.ab-line {
	stroke: currentColor;
	stroke-width: 8;
	stroke-linecap: round;
	stroke-linejoin: round;
	fill: none;
}

.ab-head {
	fill: var(--ab-head-fill, transparent);
}

.ab-smile {
	stroke-width: 7;
}

.ab-eye {
	fill: currentColor;
	stroke: none;
	transform-box: fill-box;
	transform-origin: center;
}

.ab-blush {
	stroke: currentColor;
	stroke-width: 6;
	stroke-linecap: round;
	opacity: .8;
}

.wing path {
	stroke: currentColor;
	stroke-width: 8;
	stroke-linecap: round;
	fill: none;
}

.wing-l {
	transform-origin: 58px 100px;
}

.wing-r {
	transform-origin: 142px 100px;
}

.lively .wing-l {
	animation: ab-flap-l 2.6s ease-in-out infinite;
}

.lively .wing-r {
	animation: ab-flap-r 2.6s ease-in-out infinite;
}

.lively .ab-eye {
	animation: ab-blink 4.6s ease-in-out infinite;
}

@keyframes ab-flap-l {

	0%,
	100% {
		transform: rotate(0deg);
	}

	50% {
		transform: rotate(-9deg);
	}
}

@keyframes ab-flap-r {

	0%,
	100% {
		transform: rotate(0deg);
	}

	50% {
		transform: rotate(9deg);
	}
}

@keyframes ab-blink {

	0%,
	90%,
	100% {
		transform: scaleY(1);
	}

	94% {
		transform: scaleY(.12);
	}
}

@media (prefers-reduced-motion: reduce) {

	.lively .wing-l,
	.lively .wing-r,
	.lively .ab-eye {
		animation: none;
	}
}
</style>