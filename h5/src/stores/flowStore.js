// 画布核心 Store：VueFlow 节点/边的数据源，所有节点操作均通过此 store 进行
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { applyNodeChanges, applyEdgeChanges } from '@vue-flow/core'

// ─── 唯一 ID 生成器 ──────────────────────────────────────────────────────────
let _uid = 0
// 每次调用返回 "prefix-自增序号-时间戳"，确保同一会话内全局唯一
const uid = (prefix = 'node') => `${prefix}-${++_uid}-${Date.now()}`

// 节点类型 → 显示名称映射（用于 changeNodeType 更新 label）
const TYPE_LABELS = {
  textNode:        '文本节点',
  imageUploadNode: '图片上传',
  imageGenNode:    'AI 图片',
  videoUploadNode: '视频上传',
  videoGenNode:    'AI 视频',
  noteNode:        '备注',
  groupNode:       '分组',
  // legacy 兼容旧项目数据
  imageNode: '图片节点',
  videoNode: '视频节点',
}

// 节点类型 → 默认宽度映射（用于计算新节点的连接位置偏移）
const NODE_WIDTHS = {
  textNode: 220,
  imageUploadNode: 240, imageGenNode: 240,
  videoUploadNode: 280, videoGenNode: 280,
  noteNode: 180, groupNode: 0,
  // legacy
  imageNode: 240, videoNode: 280,
}

export const useFlowStore = defineStore('flow', () => {
  // ─── 画布设置 ────────────────────────────────────────────────────────────
  const snapEnabled = ref(true)   // 是否开启对齐网格
  const gridSize    = ref(20)     // 网格尺寸（px）
  const showGrid    = ref(true)   // 是否显示背景网格

  // ─── 画布核心数据（VueFlow 受控模式数据源）────────────────────────────────
  const nodes = ref([])   // 所有节点，直接绑定到 <VueFlow :nodes>
  const edges = ref([])   // 所有边，直接绑定到 <VueFlow :edges>

  // ─── 计算属性 ────────────────────────────────────────────────────────────
  const selectedNodes = computed(() => nodes.value.filter((n) => n.selected))
  const selectedEdges = computed(() => edges.value.filter((e) => e.selected))

  // ─── VueFlow 变更处理器（受控模式必须实现）────────────────────────────────
  // VueFlow 内部产生变更（拖拽/选中/删除）后触发 @nodes-change，
  // 通过 applyNodeChanges 将变更应用到 store，保持数据一致性
  function handleNodesChange(changes) {
    nodes.value = applyNodeChanges(changes, nodes.value)
  }
  function handleEdgesChange(changes) {
    edges.value = applyEdgeChanges(changes, edges.value)
  }

  // ─── 节点工厂（各类型初始数据）─────────────────────────────────────────
  const nodeDefaults = {
    textNode: (pos) => ({
      id: uid('text'),
      type: 'textNode',
      position: pos,
      data: { label: '文本节点', content: '双击编辑内容...', outputValue: '' },
    }),
    imageNode: (pos) => ({
      id: uid('image'), type: 'imageNode', position: pos,
      data: { label: '图片节点', src: '', alt: '图片' },
    }),
    imageUploadNode: (pos) => ({
      id: uid('image'), type: 'imageUploadNode', position: pos,
      data: { label: '图片上传', src: '', alt: '图片' },
    }),
    imageGenNode: (pos) => ({
      id: uid('image'), type: 'imageGenNode', position: pos,
      data: { label: 'AI 图片', src: '', alt: '图片' },
    }),
    videoNode: (pos) => ({
      id: uid('video'), type: 'videoNode', position: pos,
      data: { label: '视频节点', src: '', poster: '' },
    }),
    videoUploadNode: (pos) => ({
      id: uid('video'), type: 'videoUploadNode', position: pos,
      data: { label: '视频上传', src: '', poster: '' },
    }),
    videoGenNode: (pos) => ({
      id: uid('video'), type: 'videoGenNode', position: pos,
      data: { label: 'AI 视频', src: '', poster: '' },
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
      zIndex: -1,              // 分组节点渲染在子节点下层
      class: 'group-node-parent',
    }),
  }

  /** 只构造节点对象，不写入 nodes.value（供外部调用 vueflow.addNodes 使用）*/
  function createNode(type, position) {
    return nodeDefaults[type]?.(position) ?? null
  }

  /** 构造节点对象并追加到 nodes 数组 */
  function addNodeOfType(type, position) {
    const node = createNode(type, position)
    if (!node) return
    nodes.value = [...nodes.value, node]  // 使用新数组触发 Vue 响应式更新
  }

  // ─── 节点数据更新 ─────────────────────────────────────────────────────
  /**
   * 更新指定节点的 data 字段，并将 outputValue/content 传播到下游边标签
   * liveNodes：VueFlow 内部节点快照（含最新拖拽坐标），异步操作后传入以避免位置重置
   */
  function updateNodeData(id, patch, liveNodes) {
    nodes.value = nodes.value.map((n) => {
      // 如果有最新坐标快照，优先使用（防止异步更新丢失拖拽位置）
      const live = liveNodes?.find((v) => v.id === n.id)
      const base = live ? { ...n, position: live.position } : n
      return base.id === id ? { ...base, data: { ...base.data, ...patch } } : base
    })
    // 数据流：当节点输出值变化时，同步更新其所有出边的标签（显示数据流向）
    if ('outputValue' in patch || 'content' in patch) {
      const val = patch.outputValue ?? patch.content ?? ''
      edges.value = edges.value.map((e) =>
        e.source === id
          ? { ...e, label: val ? String(val).slice(0, 30) : '', data: { ...e.data, value: val } }
          : e,
      )
    }
  }

  /** 更新边的属性（如样式、标签） */
  function updateEdgeData(id, patch) {
    edges.value = edges.value.map((e) =>
      e.id === id ? { ...e, ...patch } : e,
    )
  }

  /** 删除指定节点及其所有子节点（parentNode === id）和相关边 */
  function removeNodeById(id) {
    nodes.value = nodes.value.filter((n) => n.id !== id && n.parentNode !== id)
    edges.value = edges.value.filter((e) => e.source !== id && e.target !== id)
  }

  /** 删除当前所有选中节点及其相关边 */
  function removeSelectedNodes() {
    const ids = new Set(selectedNodes.value.map((n) => n.id))
    nodes.value = nodes.value.filter((n) => !ids.has(n.id))
    edges.value = edges.value.filter((e) => !ids.has(e.source) && !ids.has(e.target))
  }

  // ─── 边操作 ──────────────────────────────────────────────────────────
  /** 添加一条边，自动去重（同 source→target 不重复添加）*/
  function addEdge(edge) {
    // 防止重复连线
    const exists = edges.value.find(
      (e) => e.source === edge.source && e.target === edge.target,
    )
    if (exists) return
    edges.value = [
      ...edges.value,
      {
        ...edge,
        animated: true,                                          // 动画效果表示数据流动
        type: 'bezier',                                         // 贝塞尔曲线样式
        style: { stroke: '#646cff', strokeWidth: 2 },           // 主题色连线
        markerEnd: { type: 'arrowclosed', color: '#646cff' },   // 箭头终点
        data: { value: null },                                   // 数据流初始值
      },
    ]
  }

  // ─── 快速创建下一连接节点 ─────────────────────────────────────────────
  /**
   * 在 sourceId 节点右侧创建新节点并自动连边
   * currentPositions：当前节点位置快照（key: nodeId, value: position），
   * 防止更新 nodes 数组时丢失已拖拽节点的位置
   */
  function addConnectedNode(sourceId, newType, position, currentPositions) {
    const factory = nodeDefaults[newType]
    if (!factory) return

    let newPos = position
    if (!newPos) {
      // 没有指定位置时，自动放置在源节点右侧（宽度 + 80px 间距）
      const source = nodes.value.find((n) => n.id === sourceId)
      if (!source) return
      newPos = { x: source.position.x + (NODE_WIDTHS[source.type] ?? 220) + 80, y: source.position.y }
    }

    const newNode = factory(newPos)
    // 将 VueFlow 最新位置快照合并进 nodes 数组，与新节点一起原子更新，
    // 避免 VueFlow 收到新 prop 数组后用旧坐标重置已拖拽节点
    const base = currentPositions
      ? nodes.value.map(n => currentPositions[n.id] ? { ...n, position: currentPositions[n.id] } : n)
      : nodes.value
    nodes.value = [...base, newNode]

    // 自动连接源节点右侧 handle 到新节点左侧 handle
    addEdge({
      id: `edge-${sourceId}-${newNode.id}-${Date.now()}`,
      source: sourceId,
      sourceHandle: 'sr',   // source right
      target: newNode.id,
      targetHandle: 'tl',   // target left
    })
  }

  // ─── 节点分组 / 取消分组 ─────────────────────────────────────────────
  /** 将选中的节点（>=2个，非 groupNode 且未被分组）包裹进一个新 groupNode */
  function groupSelectedNodes() {
    // 只对顶层非分组节点操作
    const sel = selectedNodes.value.filter((n) => n.type !== 'groupNode' && !n.parentNode)
    if (sel.length < 2) return

    const PAD = 30  // 分组容器的内边距
    // 计算选中节点的包围盒
    const minX = Math.min(...sel.map((n) => n.position.x)) - PAD
    const minY = Math.min(...sel.map((n) => n.position.y)) - PAD - 24   // 额外 24px 留给标题头部
    const maxX = Math.max(...sel.map((n) => n.position.x + (n.dimensions?.width || 220))) + PAD
    const maxY = Math.max(...sel.map((n) => n.position.y + (n.dimensions?.height || 120))) + PAD

    const gid = uid('group')
    const groupNode = {
      id: gid,
      type: 'groupNode',
      position: { x: minX, y: minY },
      style: { width: `${maxX - minX}px`, height: `${maxY - minY}px` },
      data: { label: '分组' },
      zIndex: -1,                    // 分组节点置于子节点底层
      class: 'group-node-parent',
      selected: false,
    }

    const selIds = new Set(sel.map((n) => n.id))
    nodes.value = [
      groupNode,  // 先插入 groupNode 确保子节点能找到 parentNode
      ...nodes.value.map((n) => {
        if (!selIds.has(n.id)) return n
        return {
          ...n,
          parentNode: gid,          // 设置父节点
          extent: 'parent',         // 限制拖拽范围在父容器内
          // 将坐标转换为相对于 groupNode 的局部坐标
          position: { x: n.position.x - minX, y: n.position.y - minY },
          selected: false,
        }
      }),
    ]
  }

  /** 解散分组：删除 groupNode，子节点转换为局部坐标回到全局坐标 */
  function ungroupNodes(groupId) {
    const group = nodes.value.find((n) => n.id === groupId)
    if (!group) return
    nodes.value = nodes.value
      .filter((n) => n.id !== groupId)   // 移除 groupNode
      .map((n) => {
        if (n.parentNode !== groupId) return n
        return {
          ...n,
          parentNode: undefined,    // 脱离父节点
          extent: undefined,        // 解除范围限制
          // 将局部坐标转换回全局坐标
          position: {
            x: n.position.x + group.position.x,
            y: n.position.y + group.position.y,
          },
        }
      })
  }

  // ─── 节点类型切换（原地替换，保留位置）────────────────────────────────
  /** 将指定节点的类型改为 newType，尽量保留旧节点的内容数据 */
  function changeNodeType(id, newType, livePosition) {
    const idx = nodes.value.findIndex((n) => n.id === id)
    if (idx === -1) return

    const n = nodes.value[idx]
    // 从旧数据中提取可复用的字段
    const { content = '', src = '', outputValue = '' } = n.data
    const label = TYPE_LABELS[newType] ?? '节点'

    // 根据新类型构造对应的 data 结构
    let newData
    switch (newType) {
      case 'textNode':
        // 文本节点优先使用旧 content，其次是 src（如从图片切换过来）
        newData = { label, content: content || src || '双击编辑内容...', outputValue }
        break
      case 'imageNode':
      case 'imageUploadNode':
      case 'imageGenNode':
        newData = { label, src, alt: '图片' }
        break
      case 'videoNode':
      case 'videoUploadNode':
      case 'videoGenNode':
        newData = { label, src, poster: '' }
        break
      case 'noteNode':
        newData = { content: content || label, color: '#f5c542' }
        break
      default:
        newData = n.data  // 未知类型保留原始 data
    }

    // 优先使用 livePosition（VueFlow 最新坐标），避免坐标回退
    const position = livePosition ?? n.position
    // 原地替换节点（保持数组顺序稳定，避免 z-index 变化）
    nodes.value = [
      ...nodes.value.slice(0, idx),
      { ...n, type: newType, data: newData, position },
      ...nodes.value.slice(idx + 1),
    ]
  }

  // ─── 上游上下文收集（数据流分析）────────────────────────────────────────
  /**
   * 收集当前节点的上游参考上下文（用于 AI 生成的 context 参数）
   * - 直连节点（1 跳）：无论该节点 scope 为何，均纳入
   * - 间接节点（>1 跳）：仅当该节点 scope === 'global' 时纳入，并继续向上遍历
   * 返回 [{ nodeId, label, content, scope }]
   */
  function getUpstreamContext(nodeId) {
    const allEdges = edges.value
    // 构建 id → node 的快速查找表
    const nodesById = {}
    for (const n of nodes.value) nodesById[n.id] = n

    const result = []
    const visited = new Set()  // 防止环形边导致无限递归

    function traverse(id, isDirect) {
      for (const edge of allEdges) {
        if (edge.target !== id) continue  // 只关注以当前节点为目标的边
        const srcId = edge.source
        if (visited.has(srcId)) continue  // 避免重复访问
        visited.add(srcId)

        const src = nodesById[srcId]
        if (!src) continue

        const scope   = src.data?.scope ?? 'direct'
        const content = src.data?.outputValue || src.data?.content || ''

        if (isDirect) {
          // 直连节点：有内容就纳入
          if (content) result.push({ nodeId: srcId, label: src.data?.label || src.type, content, scope })
          // 直连节点若是 global scope，继续向上收集（透传上下文）
          if (scope === 'global') traverse(srcId, false)
        } else if (scope === 'global') {
          // 间接节点：只有 global scope 才纳入并继续向上
          if (content) result.push({ nodeId: srcId, label: src.data?.label || src.type, content, scope })
          traverse(srcId, false)
        }
      }
    }

    traverse(nodeId, true)
    return result
  }

  // ─── 画布加载 / 快照 ──────────────────────────────────────────────────
  // 图片/视频节点的出边不显示 label（避免干扰 URL 显示）
  const NO_LABEL_TYPES = new Set([
    'imageNode', 'imageUploadNode', 'imageGenNode',
    'videoNode', 'videoUploadNode', 'videoGenNode',
  ])

  /** 从保存的项目数据还原画布（在 FlowCanvas onMounted 前调用） */
  function loadCanvas(canvasData) {
    if (!canvasData) return
    // 支持传入字符串（直接从后端返回）或已解析的对象
    const data = typeof canvasData === 'string' ? JSON.parse(canvasData) : canvasData
    const loadedNodes = data.nodes ?? []
    // 构建节点类型快查表，用于清除媒体节点的边标签
    const nodeTypeMap = Object.fromEntries(loadedNodes.map(n => [n.id, n.type]))
    nodes.value = loadedNodes
    edges.value = (data.edges ?? []).map(e =>
      NO_LABEL_TYPES.has(nodeTypeMap[e.source]) ? { ...e, label: '' } : e
    )
  }

  /** 获取当前画布快照（用于保存到服务端） */
  function getCanvasSnapshot() {
    return { nodes: nodes.value, edges: edges.value }
  }

  /** 重置为空画布（新建项目或切换项目时使用） */
  function resetCanvas() {
    nodes.value = []
    edges.value = []
  }

  // ─── 对外暴露 ─────────────────────────────────────────────────────────
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
    createNode,
    addNodeOfType,
    updateNodeData,
    updateEdgeData,
    removeNodeById,
    removeSelectedNodes,
    addEdge,
    addConnectedNode,
    groupSelectedNodes,
    ungroupNodes,
    changeNodeType,
    getUpstreamContext,
    loadCanvas,
    getCanvasSnapshot,
    resetCanvas,
  }
})
