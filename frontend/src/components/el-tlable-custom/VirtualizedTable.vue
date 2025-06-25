<script setup lang="ts">
import { nextTick, onActivated, ref, h, computed } from 'vue';
import { onDeactivated, PropType } from "@vue/runtime-dom";
import {ElCheckbox, SortBy, TableV2SortOrder} from "element-plus";

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
        cellRenderer: ({ rowData }: any) => { // rowData는 { userId: '...', userName: '...' } 같은 객체
          const cellValue = rowData[column.dataKey];

          const formattedValue = col.formatter
              ? col.formatter(rowData, column, cellValue, -1) // -1은 인덱스를 알 수 없음을 의미
              : cellValue;

          return h('div', {
            style: {
              width: '100%',
              overflow: 'hidden',
              whiteSpace: 'nowrap',
              textOverflow: 'ellipsis',
              textAlign: col.align || 'left',
              fontSize: '12px'
            },
            title: formattedValue
          }, formattedValue);
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

  const dataColumns = transformColumns(props.columns)

  if(props.selectionMode) {
    return [selectionColumn, ...dataColumns]
  }
  else {
    return dataColumns;
  }
})

/***************************
 * 정렬 로직
 ***************************/

// 그리드 정렬 ref 정의
const sortBy = ref<SortBy>({ key: '', order: TableV2SortOrder.ASC })

/**
 * 그리드 정렬
 */
const sortedData = computed(() => {
  const { key, order } = sortBy.value;
  const multiplier = order === TableV2SortOrder.ASC ? 1 : -1;

  if(!key || !order) return [...dataWithIds.value];

  return [...dataWithIds.value].sort((a, b) => {
    const valA = a[key];
    const valB = b[key];

    return String(valA).localeCompare(String(valB), undefined, {
      numeric: true,
      sensitivity: 'base'
    }) * multiplier;
  })
})

const handleColumnSort = (newSortBy: SortBy) => {
  const currentKey = sortBy.value.key;
  const currentOrder = sortBy.value.order;
  const newKey = newSortBy.key;

  if(currentKey != newKey) {
    sortBy.value = { key: newKey, order: 'asc' as TableV2SortOrder }
  }
  else {
    if(currentOrder === 'asc') {
      sortBy.value = { key: newKey, order: 'desc' as TableV2SortOrder }
    }
    else if(currentOrder === 'desc') {
      sortBy.value = { key: '', order: '' as TableV2SortOrder }
    }
  }
}

/***************************
 * selection 로직
 ***************************/

// 각 행 선택을 위한 id 값 넣어주기
const dataWithIds = computed(() => {
  return props.data.map((item, index) => ({
    ...item,
    _internalId: `internal-id-${index}`,
    checked: item.checked ?? false,
  }))
})

// 선택된 행들만 필터링하여 보여주기
const selectedRows = computed(() => dataWithIds.value.filter(row => row.checked))

// 헤더의 '전체선택' 체크박스 상태
const isAllSelected = computed(() => {
  if(dataWithIds.value.length === 0) return false;
  return selectedRows.value.length === dataWithIds.value.length;
})

// 부분선택 상태
const containsChecked = computed(() => {
  return selectedRows.value.length > 0 && !isAllSelected.value;
})

/**
 * 전체선택 핸들러
 * @param value
 */
const handleSelectAll = (value: any) => {
  const newData = dataWithIds.value.map(row => ({
    ...row,
    checked: value,
  }))
  emitUpdate(newData);
}

/**
 * 부분선택 핸들러
 * @param rowData
 * @param value
 */
const handleRowSelect = (rowData: any, value: any) => {

  let newData;

  if(props.selectionMode === 'single') {
    const clickedId = rowData._internalId;
    const isCurrentlyChecked = rowData.checked;

    newData = dataWithIds.value.map(row => {
      return {
        ...row,
        checked: (row._internalId === clickedId) ? !isCurrentlyChecked : false
      }
    })
  }
  else {
    newData = dataWithIds.value.map(row => {
      if(row._internalId === rowData._internalId) {
        return { ...row, checked: value };
      }
      return row;
    })
  }

  emitUpdate(newData);
}

/**
 * 부모컴포넌트에 원본 데이터 전달
 * @param newData
 */
const emitUpdate = (newData: any) => {
  const originData = newData.map((item: any) => {
    const { _internalId, ...rest } = item;
    return rest;
  })
  emit('update:data', originData)
}

/**
 * selection 컬럼 정의
 */
const selectionColumn = {
  key: 'selection',
  width: 40,
  align: 'center',
  cellRenderer: ({ rowData }: any) => h(ElCheckbox,{
    modelValue: rowData.checked,
    onChange(val) {
      handleRowSelect(rowData, val);
    },
  }),
  headerCellRenderer: props.selectionMode === 'multiple'
    ? () => h(ElCheckbox, {
        modelValue: isAllSelected.value,
        ineterminate: containsChecked.value,
        onChange: handleSelectAll,
      })
      : () => null,
}

/**
 * 선택된 행 리턴
 */
const getSelectionRows = () => {
  const selectedInternalRows = selectedRows.value;
  const originalSelectedRows = selectedInternalRows.map(item => {
    const { _internalId, ...rest } = item;
    return rest;
  })
  return originalSelectedRows;
}


/**
 * 그리드 더블클릭 이벤트
 * @param data
 */
const onRowDbClick = (data: any) => {
  emit('row-dbclick', data);
}

/**
 * 그리드 핸들러
 */
const handleRowDbClick = {
  ondbclick: ({ rowData, rowIndex, rowKey }: any) => {
    const { _internalId, ...originalRowData } = rowData
    onRowDbClick(originalRowData)
  }
}

/**
 * 외부에서 함수 사용할 수 있도록 정의
 */
defineExpose(({
  getSelectionRows,
}))

</script>

<template>
  <el-auto-resizer>
    <template #default="{ height, width }">
      <el-table-v2
        ref="tableRef"
        :key="tableKey"
        :columns="v2Columns"
        :data="sortedData"
        :height="height"
        :width="width"
        :header-height="35"
        :row-height="35"
        :estimated-row-height="35"
        :sort-by="sortBy"
        :row-event-handlers="handleRowDbClick"
        @row-dbclick="onRowDbClick"
        @column-solt="handleColumnSort"
        @scroll="onTableScroll"
        fixed
      >
        <!-- No Data 영역을 위한 슬롯 추가 -->
        <template #empty>
          <div class="el-empty-custom">
            <el-text style="color: var(--el-text-color-secondary);">데이터 없음</el-text>
          </div>
        </template>
      </el-table-v2>
    </template>
  </el-auto-resizer>
</template>

<style scoped>
.el-empty-custom {
  --el-empty-padding: 20px 0;
  --el-empty-image-width: 160px;
  --el-empty-description-margin-top: 20px;
  --el-empty-bottom-margin-top: 20px;
  --el-empty-fill-color-0: var(--el-color-white);
  --el-empty-fill-color-1: #fcfcfd;
  --el-empty-fill-color-2: #f8f9fb;
  --el-empty-fill-color-3: #f7f8fc;
  --el-empty-fill-color-4: #eeeff3;
  --el-empty-fill-color-5: #edeef2;
  --el-empty-fill-color-6: #e9ebef;
  --el-empty-fill-color-7: #e5e7e9;
  --el-empty-fill-color-8: #e0e3e9;
  --el-empty-fill-color-9: #d5d7de;
  align-items: center;
  box-sizing: border-box;
  display: flex
;
  flex-direction: column;
  justify-content: center;
  padding: var(--el-empty-padding);
  text-align: center;
}
</style>