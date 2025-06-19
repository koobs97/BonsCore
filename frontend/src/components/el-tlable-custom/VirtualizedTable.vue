<script setup lang="ts">
import { nextTick, onActivated, ref, h, computed } from 'vue';
import { onDeactivated, PropType } from "@vue/runtime-dom";
import { SortBy } from "element-plus";

// ColumnDef 인터페이스
interface ColumnDef {
  prop?: string;
  label: string;
  width?: number | string;
  minWidth?: number | string;
  tooltip?: boolean;
  sortable?: boolean;
  align?: 'left' | 'center' | 'right';
  headerAlign?: 'left' | 'center' | 'right';
  formatter?: (row: any, column: any, cellValue: any, index: number) => string;
  code?: string;
  children?: ColumnDef[];
}

// props 정의
const props = defineProps({
  columns: {
    type: Array as PropType<ColumnDef[]>,
    required: true,
  },
  data: {
    type: Array as PropType<any[]>,
    required: true,
  },
  sortBy: {
    type: Object as PropType<SortBy>,
    required: false,
  },
  selectionMode: {
    type: String,
    default: undefined,
    validator: (value: any) => ['single', 'multiple'].includes(value),
  }
})

// emit 정의
const emit = defineEmits(['row-dbclick', 'column-sort', 'update:data'])

// table ref
const tableRef = ref(null) as any;

/***************************
 * 탭 이동 시 데이터 유지
 ***************************/

// 테이블 재 렌더링으로 탭 이동시 데이터 깨짐 방지
const tableKey = ref(0)

// 현재 스크롤 위치
const currentScrollTop = ref(0)
const currentScrollLeft = ref(0)

// 탭 전환 시 최종 위치
const savedScrollTop = ref(0)
const savedScrollLeft = ref(0)

// 현재 스크롤 위치
const onTableScroll = ({ scrollTop, scrollLeft }: any) => {
  currentScrollTop.value = scrollTop;
  currentScrollLeft.value = scrollLeft;
}

// 탭 활성화 이벤트
onActivated( async () => {
  await nextTick( async () => {

    tableKey.value += 1;

    if((savedScrollTop.value > 0 || savedScrollLeft.value > 0) && tableRef.value) {
      await nextTick()
      tableRef.value.scrollTo({
        scrollTop: savedScrollTop.value,
        scrollLeft: savedScrollLeft.value
      })
    }

  })
})

// 탭 닫을 때 커서 위치 저장
onDeactivated(() => {
  savedScrollTop.value = currentScrollTop.value;
  savedScrollLeft.value = currentScrollLeft.value;
})

const v2Columns = computed(() => {
  const transformColumns = (defs: ColumnDef[]): any[] => {
    return defs.map(col => {
      const column: any = {
        key: col.prop || col.label,
        dataKey: col.prop || '',
        title: col.label,
        width: Number(col.width) || 150,
        minWidth: Number(col.minWidth) || 80,
        sortable: col.sortable ?? false,
        align: col.align || 'left',
        headerAlign: col.headerAlign || 'center',
        cellRenderer: ({ rowData }: any) => {
          return h('div', {
            style: {
              width: '100%',
              overflow: 'hidden',
              whiteSpace: 'nowrap',
              textOverflow: 'ellipsis',
              textAlign: col.align || 'left',
              fontSize: '12px'
            },
            title: rowData
          }, rowData);
        },
        headerCellRenderer: ({ column }: any) => {
          const headerStyle = {
            width: '100%',
            textAlign: col.headerAlign || 'center'
          }
          return h(
              'div',
              { style: headerStyle },
              column.title
          )
        },
        rowColumnDef: col
      };

      return column;
    });
  };
})

</script>

<template>
  <el-auto-resizer>
    <templage #default="{ height, width }">
      <el-table-v2
        ref="tableRef"
        :key="tableKey"
        :height="height"
        :width="width"
      />
    </templage>
  </el-auto-resizer>
</template>

<style scoped>

</style>