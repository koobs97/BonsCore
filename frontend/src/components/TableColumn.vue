<script setup lang="ts">
import { onMounted } from 'vue';

interface ColumnDef {
    prop?: string;
    label: string;
    width?: number | string;
    minWidth?: number | string;
    tooltip?: boolean;
    sortable?: boolean;
    align?: 'left' | 'center' | 'right';
    formatter?: (row: any, column: any, cellValue: any, index: number) => string;
    code?: string;
    children?: ColumnDef[];
}

const props = defineProps<{ columns: ColumnDef[] }>();

onMounted(() => {
    setTimeout(()=>{console.log('📐 FiveLevelTableColumn props.columns:', JSON.stringify(props.columns, null, 2));}, 500)
});

const getSortable = (col: ColumnDef): boolean => {
    return col.children ? false : col.sortable ?? true;
};
</script>

<template>
    <template v-for="(col1, i1) in props.columns" :key="'l1-' + i1">
        <el-table-column
            :prop="col1.prop"
            :label="col1.label"
            :width="col1.width"
            :min-width="col1.minWidth"
            :sortable="getSortable(col1)"
            :show-overflow-tooltip="col1.tooltip ?? false"
            :align="col1.align ?? 'left'"
            :formatter="!col1.code ? col1.formatter : undefined"
        >
<!--            <template v-if="col1.code" #default="{ row }">-->
<!--                <span>{{ Common.getCodeName(col1.code, row[col1.prop]) }}</span>-->
<!--            </template>-->

            <template v-if="col1.children" #default>
                <template v-for="(col2, i2) in col1.children" :key="'l2-' + i1 + '-' + i2">
                    <el-table-column
                        :prop="col2.prop"
                        :label="col2.label"
                        :width="col2.width"
                        :min-width="col2.minWidth"
                        :sortable="getSortable(col2)"
                        :show-overflow-tooltip="col2.tooltip ?? false"
                        :align="col2.align ?? 'left'"
                        :formatter="!col2.code ? col2.formatter : undefined"
                    >
<!--                        <template v-if="col2.code" #default="{ row }">-->
<!--                            <span>{{ Common.getCodeName(col2.code, row[col2.prop]) }}</span>-->
<!--                        </template>-->

                        <template v-if="col2.children" #default>
                            <template v-for="(col3, i3) in col2.children" :key="'l3-' + i1 + '-' + i2 + '-' + i3">
                                <el-table-column
                                    :prop="col3.prop"
                                    :label="col3.label"
                                    :width="col3.width"
                                    :min-width="col3.minWidth"
                                    :sortable="getSortable(col3)"
                                    :show-overflow-tooltip="col3.tooltip ?? false"
                                    :align="col3.align ?? 'left'"
                                    :formatter="!col3.code ? col3.formatter : undefined"
                                >
<!--                                    <template v-if="col3.code" #default="{ row }">-->
<!--                                        <span>{{ Common.getCodeName(col3.code, row[col3.prop]) }}</span>-->
<!--                                    </template>-->

                                    <template v-if="col3.children" #default>
                                        <template v-for="(col4, i4) in col3.children" :key="'l4-' + i1 + '-' + i2 + '-' + i3 + '-' + i4">
                                            <el-table-column
                                                :prop="col4.prop"
                                                :label="col4.label"
                                                :width="col4.width"
                                                :min-width="col4.minWidth"
                                                :sortable="getSortable(col4)"
                                                :show-overflow-tooltip="col4.tooltip ?? false"
                                                :align="col4.align ?? 'left'"
                                                :formatter="!col4.code ? col4.formatter : undefined"
                                            >
<!--                                                <template v-if="col4.code" #default="{ row }">-->
<!--                                                    <span>{{ Common.getCodeName(col4.code, row[col4.prop]) }}</span>-->
<!--                                                </template>-->

                                                <template v-if="col4.children" #default>
                                                    <template v-for="(col5, i5) in col4.children" :key="'l5-' + i1 + '-' + i2 + '-' + i3 + '-' + i4 + '-' + i5">
                                                        <el-table-column
                                                            :prop="col5.prop"
                                                            :label="col5.label"
                                                            :width="col5.width"
                                                            :min-width="col5.minWidth"
                                                            :sortable="getSortable(col5)"
                                                            :show-overflow-tooltip="col5.tooltip ?? false"
                                                            :align="col5.align ?? 'left'"
                                                            :formatter="!col5.code ? col5.formatter : undefined"
                                                        >
<!--                                                            <template v-if="col5.code" #default="{ row }">-->
<!--                                                                <span>{{ Common.getCodeName(col5.code, row[col5.prop]) }}</span>-->
<!--                                                            </template>-->
                                                        </el-table-column>
                                                    </template>
                                                </template>
                                            </el-table-column>
                                        </template>
                                    </template>
                                </el-table-column>
                            </template>
                        </template>
                    </el-table-column>
                </template>
            </template>
        </el-table-column>
    </template>
</template>
