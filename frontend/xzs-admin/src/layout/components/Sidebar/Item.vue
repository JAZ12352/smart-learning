<script>
export default {
  name: 'MenuItem',
  functional: true,
  props: {
    icon: {
      type: String,
      default: ''
    },
    title: {
      type: String,
      default: ''
    }
  },
  render (h, context) {
    const { icon, title } = context.props
    const vnodes = []

    if (icon) {
      vnodes.push(
        <span class="menu-icon-wrap">
          <svg-icon icon-class={icon} class="menu-icon-svg" />
        </span>
      )
    }

    if (title) {
      vnodes.push(
        <span slot='title' class="menu-title-text">{(title)}</span>
      )
    }
    return vnodes
  }
}
</script>

<style scoped>
.menu-icon-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  margin-right: 12px;
  flex-shrink: 0;
}

.menu-icon-svg {
  font-size: 18px;
  transition: all 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.menu-title-text {
  font-size: 13.5px;
  letter-spacing: 0.4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
