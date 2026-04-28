import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { applyNodeChanges, applyEdgeChanges } from '@vue-flow/core'

let _uid = 0
const uid = (prefix = 'node') => `${prefix}-${++_uid}-${Date.now()}`

export const useFlowStore = defineStore('flow', () => {
  // ─── Canvas settings ────────────────────────────────────────────────
  const snapEnabled = ref(true)
  const gridSize = ref(20)
  const showGrid = ref(true)

  // ─── Initial nodes ───────────────────────────────────────────────────
  const nodes = ref([
    {
      id: 'text-1',
      type: 'textNode',
      position: { x: 180, y: 120 },
      data: { label: '文本节点', content: '这是一段示例文本内容，双击可以编辑。', outputValue: '' },
    },
    {
      id: 'image-1',
      type: 'imageNode',
      position: { x: 500, y: 80 },
      data: {
        label: '图片节点',
        src: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=240&fit=crop',
        alt: '示例图片',
      },
    },
    {
      id: 'video-1',
      type: 'videoNode',
      position: { x: 500, y: 300 },
      data: { label: '视频节点', src: '', poster: '' },
    },
    {
      id: 'note-1',
      type: 'noteNode',
      position: { x: 180, y: 360 },
      data: { content: '📌 这是一条备注\n可以记录想法或说明', color: '#f5c542' },
    },
  ])

  // ─── Initial edges ───────────────────────────────────────────────────
  const edges = ref([
    {
      id: 'e-text-image',
      source: 'text-1',
      target: 'image-1',
      animated: true,
      type: 'smoothstep',
      label: '',
      data: { value: null },
      style: { stroke: '#646cff', strokeWidth: 2 },
      markerEnd: { type: 'arrowclosed', color: '#646cff' },
    },
  ])

  // ─── Computed ────────────────────────────────────────────────────────
  const selectedNodes = computed(() => nodes.value.filter((n) => n.selected))
  const selectedEdges = computed(() => edges.value.filter((e) => e.selected))

  // ─── VueFlow change handlers (controlled pattern) ────────────────────
  function handleNodesChange(changes) {
    nodes.value = applyNodeChanges(changes, nodes.value)
  }

  function handleEdgesChange(changes) {
    edges.value = applyEdgeChanges(changes, edges.value)
  }

  // ─── Node factory ────────────────────────────────────────────────────
  const nodeDefaults = {
    textNode: (pos) => ({
      id: uid('text'),
      type: 'textNode',
      position: pos,
      data: { label: '文本节点', content: '双击编辑内容...', outputValue: '' },
    }),
    imageNode: (pos) => ({
      id: uid('image'),
      type: 'imageNode',
      position: pos,
      data: { label: '图片节点', src: '', alt: '图片' },
    }),
    videoNode: (pos) => ({
      id: uid('video'),
      type: 'videoNode',
      position: pos,
      data: { label: '视频节点', src: '', poster: '' },
    }),
    noteNode: (pos) => ({
      id: uid('note'),
      type: 'noteNode',
      position: pos,
      data: { content: '备注内容...', color: '#f5c542' },
    }),
    groupNode: (pos, w = 420, h = 280) => ({
      id: uid('group'),
      type: 'groupNode',
      position: pos,
      style: { width: `${w}px`, height: `${h}px` },
      data: { label: '分组' },
      zIndex: -1,
      class: 'group-node-parent',
    }),
  }

  function addNodeOfType(type, position) {
    const factory = nodeDefaults[type]
    if (!factory) return
    nodes.value = [...nodes.value, factory(position)]
  }

  // ─── Data updates ────────────────────────────────────────────────────
  function updateNodeData(id, patch) {
    nodes.value = nodes.value.map((n) =>
      n.id === id ? { ...n, data: { ...n.data, ...patch } } : n,
    )
    // Propagate outputValue to connected edges labels
    if ('outputValue' in patch || 'content' in patch) {
      const val = patch.outputValue ?? patch.content ?? ''
      edges.value = edges.value.map((e) =>
        e.source === id
          ? { ...e, label: val ? String(val).slice(0, 30) : '', data: { ...e.data, value: val } }
          : e,
      )
    }
  }

  function updateEdgeData(id, patch) {
    edges.value = edges.value.map((e) =>
      e.id === id ? { ...e, ...patch } : e,
    )
  }

  function removeNodeById(id) {
    nodes.value = nodes.value.filter((n) => n.id !== id && n.parentNode !== id)
    edges.value = edges.value.filter((e) => e.source !== id && e.target !== id)
  }

  function removeSelectedNodes() {
    const ids = new Set(selectedNodes.value.map((n) => n.id))
    nodes.value = nodes.value.filter((n) => !ids.has(n.id))
    edges.value = edges.value.filter((e) => !ids.has(e.source) && !ids.has(e.target))
  }

  function addEdge(edge) {
    // Prevent duplicate edges
    const exists = edges.value.find(
      (e) => e.source === edge.source && e.target === edge.target,
    )
    if (exists) return
    edges.value = [
      ...edges.value,
      {
        ...edge,
        animated: true,
        type: 'smoothstep',
        style: { stroke: '#646cff', strokeWidth: 2 },
        markerEnd: { type: 'arrowclosed', color: '#646cff' },
        data: { value: null },
      },
    ]
  }

  // ─── Group selected nodes ────────────────────────────────────────────
  function groupSelectedNodes() {
    const sel = selectedNodes.value.filter((n) => n.type !== 'groupNode' && !n.parentNode)
    if (sel.length < 2) return

    const PAD = 30
    const minX = Math.min(...sel.map((n) => n.position.x)) - PAD
    const minY = Math.min(...sel.map((n) => n.position.y)) - PAD - 24
    const maxX = Math.max(...sel.map((n) => n.position.x + (n.dimensions?.width || 220))) + PAD
    const maxY = Math.max(...sel.map((n) => n.position.y + (n.dimensions?.height || 120))) + PAD

    const gid = uid('group')
    const gw = maxX - minX
    const gh = maxY - minY

    const groupNode = {
      id: gid,
      type: 'groupNode',
      position: { x: minX, y: minY },
      style: { width: `${gw}px`, height: `${gh}px` },
      data: { label: '分组' },
      zIndex: -1,
      class: 'group-node-parent',
      selected: false,
    }

    const selIds = new Set(sel.map((n) => n.id))
    nodes.value = [
      groupNode,
      ...nodes.value.map((n) => {
        if (!selIds.has(n.id)) return n
        return {
          ...n,
          parentNode: gid,
          extent: 'parent',
          position: { x: n.position.x - minX, y: n.position.y - minY },
          selected: false,
        }
      }),
    ]
  }

  function ungroupNodes(groupId) {
    const group = nodes.value.find((n) => n.id === groupId)
    if (!group) return
    nodes.value = nodes.value
      .filter((n) => n.id !== groupId)
      .map((n) => {
        if (n.parentNode !== groupId) return n
        return {
          ...n,
          parentNode: undefined,
          extent: undefined,
          position: {
            x: n.position.x + group.position.x,
            y: n.position.y + group.position.y,
          },
        }
      })
  }

  return {
    nodes,
    edges,
    snapEnabled,
    gridSize,
    showGrid,
    selectedNodes,
    selectedEdges,
    handleNodesChange,
    handleEdgesChange,
    addNodeOfType,
    updateNodeData,
    updateEdgeData,
    removeNodeById,
    removeSelectedNodes,
    addEdge,
    groupSelectedNodes,
    ungroupNodes,
  }
})
