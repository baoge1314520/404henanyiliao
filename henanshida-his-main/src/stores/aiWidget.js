import { defineStore } from 'pinia'

// 悬浮「天使宝宝ai」的全局开关 / 未读 / 静音状态
const MUTE_KEY = 'angel-ai-muted'

export const useAiWidgetStore = defineStore('aiWidget', {
	state: () => ({
		visible: false,
		unread: 0,
		muted: localStorage.getItem(MUTE_KEY) === '1'
	}),
	actions: {
		open() {
			this.visible = true
			this.unread = 0
		},
		close() {
			this.visible = false
		},
		toggle() {
			if (this.visible) this.close()
			else this.open()
		},
		bump() {
			if (!this.visible) this.unread++
		},
		setMuted(v) {
			this.muted = !!v
			localStorage.setItem(MUTE_KEY, v ? '1' : '0')
		},
		toggleMuted() {
			this.setMuted(!this.muted)
		}
	}
})