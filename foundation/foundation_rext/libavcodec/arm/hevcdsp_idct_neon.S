#include "libavutil/arm/asm.S"

.macro tx4_scale src_d0, src_d1, src_d2, src_d3, shift

		vshll.s16	q6, \src_d0, #6			@64*src0
		vshll.s16	q7, \src_d2, #6			@64*src2
		vadd.s32	q4, q6, q7				@e0 = 64*(src0+src2)
		vsub.s32	q6, q6, q7				@e1 = 64*(src0-src2)
		vmull.s16	q5, \src_d1, d0[0]		@83*src1
		vmlal.s16	q5, \src_d3, d0[1]		@o0 = 83*src1 + 36*src3
		vmull.s16	q7, \src_d1, d0[1]		@36*src1
		vmlsl.s16	q7, \src_d3, d0[0]		@o1 = 36*src1 - 83*src3
		
		vadd.s32	q2, q4, q5
		vsub.s32	q5, q4, q5
		vadd.s32	q3, q6, q7
		vsub.s32	q4, q6, q7
		
		vqrshrn.s32	\src_d0, q2, \shift
		vqrshrn.s32	\src_d1, q3, \shift
		vqrshrn.s32	\src_d2, q4, \shift
		vqrshrn.s32	\src_d3, q5, \shift
.endm
#====================================================================
#void transform_4x4_add(
#r0:    uint8_t *_dst, 
#r1:	int16_t *coeffs, 
#r2:	ptrdiff_t _stride, 
#r3:	int col_limit);
#====================================================================
function ff_hevc_transform_4x4_add_neon, export=1
#	stmfd	sp!, {r4 - r6, lr}
		vld1.16		{q14, q15}, [r1]		@coeffs
		adr		r3, Tx4_idx
		vld1.16		{d0}, [r3]			@83 36
		
		tx4_scale   d28, d29, d30, d31, #7
		
		vtrn.16		d28, d29
		vtrn.16		d30, d31
		vtrn.32		q14, q15
		
		tx4_scale   d28, d29, d30, d31, #12
		
		vtrn.16		d28, d29
		vtrn.16		d30, d31
		vtrn.32		q14, q15
		
		mov		r12, r0
		vld1.32		{d4[0]}, [r12], r2			
		vld1.32		{d4[1]}, [r12], r2			
		vld1.32		{d5[0]}, [r12], r2			
		vld1.32		{d5[1]}, [r12], r2			
		vaddw.u8	q14, q14, d4
		vaddw.u8	q15, q15, d5
		vqmovun.s16	d28, q14
		vqmovun.s16	d30, q15
		vst1.32		{d28[0]}, [r0], r2
		vst1.32		{d28[1]}, [r0], r2
		vst1.32		{d30[0]}, [r0], r2
		vst1.32		{d30[1]}, [r0]
		
		mov		pc, lr
		
#	ldmfd	sp!, {r4 - r6, pc}
endfunc

.macro tx4_set src_d0, src_d1, src_d2, src_d3

		vshll.s16	q6, \src_d0, #6			@64*src0
		vshll.s16	q7, \src_d2, #6			@64*src2
		vadd.s32	q4, q6, q7				@e0 = 64*(src0+src2)
		vsub.s32	q6, q6, q7				@e1 = 64*(src0-src2)
		vmull.s16	q5, \src_d1, d0[0]		@83*src1
		vmlal.s16	q5, \src_d3, d0[1]		@o0 = 83*src1 + 36*src3
		vmull.s16	q7, \src_d1, d0[1]		@36*src1
		vmlsl.s16	q7, \src_d3, d0[0]		@o1 = 36*src1 - 83*src3
		
		vadd.s32	q2, q4, q5
		vsub.s32	q5, q4, q5
		vadd.s32	q3, q6, q7
		vsub.s32	q4, q6, q7
.endm

.macro tx8x4_shift src_d0, src_d1, src_d2, src_d3, src_d4, src_d5, src_d6, src_d7, shift
					
		tx4_set   \src_d0, \src_d2, \src_d4, \src_d6
		
		vmull.s16	q6, \src_d1, d1[0]
		vmlal.s16	q6, \src_d3, d1[1]
		vmlal.s16	q6, \src_d5, d1[2]
		vmlal.s16	q6, \src_d7, d1[3]			@o_8[0]
		vadd.s32	q7, q2, q6				@e_16[0]
		vsub.s32	q2, q2, q6 				@e_16[7]
		vqrshrn.s32	\src_d0, q7, \shift			@0‘
		
		vmull.s16	q6, \src_d1, d1[1]
		vmlsl.s16	q6, \src_d3, d1[3]
		vmlsl.s16	q6, \src_d5, d1[0]
		vmlsl.s16	q6, \src_d7, d1[2]			@o_8[1]
		vsub.s32	q7, q3, q6 				@e_16[6]
		vadd.s32	q3, q3, q6 				@e_16[1]
		vqrshrn.s32	\src_d6, q7, \shift			@6‘
		
		vmull.s16	q6, \src_d1, d1[2]
		vmlsl.s16	q6, \src_d3, d1[0]
		vmlal.s16	q6, \src_d5, d1[3]
		vmlal.s16	q6, \src_d7, d1[1]			@o_8[2]
		vadd.s32	q7, q4, q6				@e_16[2]
		vsub.s32	q4, q4, q6 				@e_16[5]
		vqrshrn.s32	\src_d2, q7, \shift			@2’
		
		vmull.s16	q6, \src_d1, d1[3]
		vmlsl.s16	q6, \src_d3, d1[2]
		vmlal.s16	q6, \src_d5, d1[1]
		vmlsl.s16	q6, \src_d7, d1[0]			@o_8[3]
		vadd.s32	q7, q5, q6				@e_16[3]
		vsub.s32	q5, q5, q6 				@e_16[4]
		vqrshrn.s32	\src_d3, q7, \shift			@3‘
		vqrshrn.s32	\src_d4, q5, \shift			@4’
		
		vqrshrn.s32	\src_d1, q3, \shift			@1’
		vqrshrn.s32	\src_d5, q4, \shift			@5‘
		vqrshrn.s32	\src_d7, q2, \shift			@7’
		
.endm
.macro	transpose8x8_short
		vtrn.16		q8, q9
		vtrn.16		q10, q11
		vtrn.16		q12, q13
		vtrn.16		q14, q15
		vtrn.32		q8,  q10
		vtrn.32		q12, q14
		vtrn.32		q9,  q11
		vtrn.32		q13, q15
		vswp		d17, d24
		vswp		d19, d26
		vswp		d21, d28
		vswp		d23, d30
.endm
#====================================================================
#void transform_8x8_add(
#r0:    uint8_t *_dst, 
#r1:	int16_t *coeffs, 
#r2:	ptrdiff_t _stride, 
#r3:	int col_limit);
#====================================================================
function ff_hevc_transform_8x8_add_neon, export=1
#	stmfd	sp!, {r4 - r6, lr}
		adr		r3, Tx4_idx
		
		vld1.16		{q0}, [r3]				@d0:83 36 0 0   d1:89 75 50 18
		vld1.16		{q8, q9}, [r1]!
		vld1.16		{q10, q11}, [r1]!
		vld1.16		{q12, q13}, [r1]!
		vld1.16		{q14, q15}, [r1]!
		
		tx8x4_shift		d16, d18, d20, d22, d24, d26, d28, d30, #7
		tx8x4_shift		d17, d19, d21, d23, d25, d27, d29, d31, #7
#---------------------------------------------------------------------		
		transpose8x8_short
		
		tx8x4_shift		d16, d18, d20, d22, d24, d26, d28, d30, #12
		tx8x4_shift		d17, d19, d21, d23, d25, d27, d29, d31, #12
		mov		r3, r0
		vld1.8		{d0}, [r3], r2
		vld1.8		{d1}, [r3], r2
		vld1.8		{d2}, [r3], r2
		vld1.8		{d3}, [r3], r2
		vld1.8		{d4}, [r3], r2
		vld1.8		{d5}, [r3], r2
		vld1.8		{d6}, [r3], r2
		vld1.8		{d7}, [r3], r2
		
		transpose8x8_short
		
		vaddw.u8	q8, q8, d0
		vaddw.u8	q9, q9, d1
		vaddw.u8	q10, q10, d2
		vaddw.u8	q11, q11, d3
		vaddw.u8	q12, q12, d4
		vaddw.u8	q13, q13, d5
		vaddw.u8	q14, q14, d6
		vaddw.u8	q15, q15, d7
		vqmovun.s16	d0, q8
		vqmovun.s16	d1, q9
		vqmovun.s16	d2, q10
		vqmovun.s16	d3, q11
		vqmovun.s16	d4, q12
		vqmovun.s16	d5, q13
		vqmovun.s16	d6, q14
		vqmovun.s16	d7, q15
		vst1.8		{d0}, [r0], r2
		vst1.8		{d1}, [r0], r2
		vst1.8		{d2}, [r0], r2
		vst1.8		{d3}, [r0], r2
		vst1.8		{d4}, [r0], r2
		vst1.8		{d5}, [r0], r2
		vst1.8		{d6}, [r0], r2
		vst1.8		{d7}, [r0], r2
		
		mov		pc, lr
#	ldmfd	sp!, {r4 - r6, pc}
endfunc
	
Tx4_idx:
.short 83, 36, 0, 0
Tx8_idx:
.short 89, 75, 50, 18
Tx16_idx:
.short 90, 87, 80, 70, 57, 43, 25, 9
Tx32_idx:
.short 90, 90, 88, 85, 82, 78, 73, 67, 61, 54, 46, 38, 31, 22, 13, 4

.macro tx8x4_set src_d0, src_d1, src_d2, src_d3, src_d4, src_d5, src_d6, src_d7
					
		tx4_set   \src_d0, \src_d2, \src_d4, \src_d6
		
		vmull.s16	q6, \src_d1, d1[0]
		vmlal.s16	q6, \src_d3, d1[1]
		vmlal.s16	q6, \src_d5, d1[2]
		vmlal.s16	q6, \src_d7, d1[3]			@o_8[0]
		vmull.s16	q7, \src_d1, d1[1]
		vmlsl.s16	q7, \src_d3, d1[3]
		vmlsl.s16	q7, \src_d5, d1[0]
		vmlsl.s16	q7, \src_d7, d1[2]			@o_8[1]
		vmull.s16	q8, \src_d1, d1[2]
		vmlsl.s16	q8, \src_d3, d1[0]
		vmlal.s16	q8, \src_d5, d1[3]
		vmlal.s16	q8, \src_d7, d1[1]			@o_8[2]
		vmull.s16	q9, \src_d1, d1[3]
		vmlsl.s16	q9, \src_d3, d1[2]
		vmlal.s16	q9, \src_d5, d1[1]
		vmlsl.s16	q9, \src_d7, d1[0]			@o_8[3]
		
		vadd.s32	q10, q2, q6				@e_16[0]
		vsub.s32	q2, q2, q6 				@e_16[7]
		vadd.s32	q6, q3, q7 				@e_16[1]
		vsub.s32	q3, q3, q7 				@e_16[6]
		vadd.s32	q7, q4, q8				@e_16[2]
		vsub.s32	q4, q4, q8 				@e_16[5]
		vadd.s32	q8, q5, q9				@e_16[3]
		vsub.s32	q5, q5, q9 				@e_16[4]
		
.endm

.macro tx_o16_0	dq0, sd0, sd1, sd2, sd3, sd4, sd5, sd6, sd7, idx_d0, idx_d1
		vmull.s16	\dq0, \sd0, \idx_d0[0]
		vmlal.s16	\dq0, \sd1, \idx_d0[1]
		vmlal.s16	\dq0, \sd2, \idx_d0[2]
		vmlal.s16	\dq0, \sd3, \idx_d0[3]
		vmlal.s16	\dq0, \sd4, \idx_d1[0]
		vmlal.s16	\dq0, \sd5, \idx_d1[1]
		vmlal.s16	\dq0, \sd6, \idx_d1[2]
		vmlal.s16	\dq0, \sd7, \idx_d1[3]			@o_16[0]
.endm

.macro tx_o16_1	dq0, sd0, sd1, sd2, sd3, sd4, sd5, sd6, sd7, idx_d0, idx_d1
		vmull.s16	\dq0, \sd0, \idx_d0[1]			@ 87*src1
		vmlal.s16	\dq0, \sd1, \idx_d1[0]			@ 57*src3
		vmlal.s16	\dq0, \sd2, \idx_d1[3]			@  9*src5
		vmlsl.s16	\dq0, \sd3, \idx_d1[1]			@-43*src7
		vmlsl.s16	\dq0, \sd4, \idx_d0[2]			@-80*src9
		vmlsl.s16	\dq0, \sd5, \idx_d0[0]			@-90*src11
		vmlsl.s16	\dq0, \sd6, \idx_d0[3]			@-70*src13
		vmlsl.s16	\dq0, \sd7, \idx_d1[2]			@-25*src15	@o_16[1]
.endm

.macro tx_o16_2	dq0, sd0, sd1, sd2, sd3, sd4, sd5, sd6, sd7, idx_d0, idx_d1
		vmull.s16	\dq0, \sd0, \idx_d0[2]			@ 80*src1
		vmlal.s16	\dq0, \sd1, \idx_d1[3]			@  9*src3
		vmlsl.s16	\dq0, \sd2, \idx_d0[3]			@-70*src5
		vmlsl.s16	\dq0, \sd3, \idx_d0[1]			@-87*src7
		vmlsl.s16	\dq0, \sd4, \idx_d1[2]			@-25*src9
		vmlal.s16	\dq0, \sd5, \idx_d1[0]			@ 57*src11
		vmlal.s16	\dq0, \sd6, \idx_d0[0]			@ 90*src13
		vmlal.s16	\dq0, \sd7, \idx_d1[1]			@ 43*src15	@o_16[2]
.endm

.macro tx_o16_3	dq0, sd0, sd1, sd2, sd3, sd4, sd5, sd6, sd7, idx_d0, idx_d1
		vmull.s16	\dq0, \sd0, \idx_d0[3]			@ 70*src1
		vmlsl.s16	\dq0, \sd1, \idx_d1[1]			@-43*src3
		vmlsl.s16	\dq0, \sd2, \idx_d0[1]			@-87*src5
		vmlal.s16	\dq0, \sd3, \idx_d1[3]			@  9*src7
		vmlal.s16	\dq0, \sd4, \idx_d0[0]			@ 90*src9
		vmlal.s16	\dq0, \sd5, \idx_d1[2]			@ 25*src11
		vmlsl.s16	\dq0, \sd6, \idx_d0[2]			@-80*src13
		vmlsl.s16	\dq0, \sd7, \idx_d1[0]			@-57*src15	@o_16[3]
.endm

.macro tx_o16_4	dq0, sd0, sd1, sd2, sd3, sd4, sd5, sd6, sd7, idx_d0, idx_d1
		vmull.s16	\dq0, \sd0, \idx_d1[0]			@ 57*src1
		vmlsl.s16	\dq0, \sd1, \idx_d0[2]			@-80*src3
		vmlsl.s16	\dq0, \sd2, \idx_d1[2]			@-25*src5
		vmlal.s16	\dq0, \sd3, \idx_d0[0]			@ 90*src7
		vmlsl.s16	\dq0, \sd4, \idx_d1[3]			@- 9*src9
		vmlsl.s16	\dq0, \sd5, \idx_d0[1]			@-87*src11
		vmlal.s16	\dq0, \sd6, \idx_d1[1]			@ 43*src13
		vmlal.s16	\dq0, \sd7, \idx_d0[3]			@ 70*src15	@o_16[4]
.endm

.macro tx_o16_5	dq0, sd0, sd1, sd2, sd3, sd4, sd5, sd6, sd7, idx_d0, idx_d1
		vmull.s16	\dq0, \sd0, \idx_d1[1]			@ 43*src1
		vmlsl.s16	\dq0, \sd1, \idx_d0[0]			@-90*src3
		vmlal.s16	\dq0, \sd2, \idx_d1[0]			@ 57*src5
		vmlal.s16	\dq0, \sd3, \idx_d1[2]			@ 25*src7
		vmlsl.s16	\dq0, \sd4, \idx_d0[1]			@-87*src9
		vmlal.s16	\dq0, \sd5, \idx_d0[3]			@ 70*src11
		vmlal.s16	\dq0, \sd6, \idx_d1[3]			@  9*src13
		vmlsl.s16	\dq0, \sd7, \idx_d0[2]			@-80*src15	@o_16[5]
.endm

.macro tx_o16_6	dq0, sd0, sd1, sd2, sd3, sd4, sd5, sd6, sd7, idx_d0, idx_d1
		vmull.s16	\dq0, \sd0, \idx_d1[2]			@ 25*src1
		vmlsl.s16	\dq0, \sd1, \idx_d0[3]			@-70*src3
		vmlal.s16	\dq0, \sd2, \idx_d0[0]			@ 90*src5
		vmlsl.s16	\dq0, \sd3, \idx_d0[2]			@-80*src7
		vmlal.s16	\dq0, \sd4, \idx_d1[1]			@ 43*src9
		vmlal.s16	\dq0, \sd5, \idx_d1[3]			@  9*src11
		vmlsl.s16	\dq0, \sd6, \idx_d1[0]			@-57*src13
		vmlal.s16	\dq0, \sd7, \idx_d0[1]			@ 87*src15	@o_16[6]
.endm

.macro tx_o16_7	dq0, sd0, sd1, sd2, sd3, sd4, sd5, sd6, sd7, idx_d0, idx_d1
		vmull.s16	\dq0, \sd0, \idx_d1[3]			@ 9*src1
		vmlsl.s16	\dq0, \sd1, \idx_d1[2]			@-25*src3
		vmlal.s16	\dq0, \sd2, \idx_d1[1]			@ 43*src5
		vmlsl.s16	\dq0, \sd3, \idx_d1[0]			@-57*src7
		vmlal.s16	\dq0, \sd4, \idx_d0[3]			@ 70*src9
		vmlsl.s16	\dq0, \sd5, \idx_d0[2]			@-80*src11
		vmlal.s16	\dq0, \sd6, \idx_d0[1]			@ 87*src13
		vmlsl.s16	\dq0, \sd7, \idx_d0[0]			@-90*src15	@o_16[7]
.endm

.macro tx16x4_v
											
		mov		lr, r1
		vld1.16		{d24}, [lr], r12		@src0
		vld1.16		{d25}, [lr], r12		@src2
		vld1.16		{d26}, [lr], r12		@src4
		vld1.16		{d27}, [lr], r12		@src6
		vld1.16		{d28}, [lr], r12		@src8
		vld1.16		{d29}, [lr], r12		@src10
		vld1.16		{d30}, [lr], r12		@src12
		vld1.16		{d31}, [lr], r12		@src14
		
		tx8x4_set	d24, d25, d26, d27, d28, d29, d30, d31
		
		add		lr, r1, r6
		vld1.16		{d24}, [lr], r12		@src1
		vld1.16		{d25}, [lr], r12		@src3
		vld1.16		{d26}, [lr], r12		@src5
		vld1.16		{d27}, [lr], r12		@src7
		vld1.16		{d28}, [lr], r12		@src9
		vld1.16		{d29}, [lr], r12		@src11
		vld1.16		{d30}, [lr], r12		@src13
		vld1.16		{d31}, [lr], r12		@src15

		tx_o16_0	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[0]
		mov		lr, r1
		vadd.s32	q9,  q10, q11
		vqrshrn.s32	d18, q9, #7
		vst1.16		{d18}, [lr], r6			@0'
		vsub.s32	q10, q10, q11			@15'
		
		tx_o16_1	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[1]
		vadd.s32	q9, q6, q11
		vqrshrn.s32	d18, q9, #7
		vst1.16		{d18}, [lr], r6			@1'
		vsub.s32	q6, q6, q11				@14'
		
		tx_o16_2	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[2]
		vadd.s32	q9, q7, q11
		vqrshrn.s32	d18, q9, #7
		vst1.16		{d18}, [lr], r6			@2'
		vsub.s32	q7, q7, q11				@13'
		
		tx_o16_3	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[3]
		vadd.s32	q9, q8, q11
		vqrshrn.s32	d18, q9, #7
		vst1.16		{d18}, [lr], r6			@3'
		vsub.s32	q8, q8, q11				@12'
		
		tx_o16_4	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[4]
		vadd.s32	q9, q5, q11
		vqrshrn.s32	d18, q9, #7
		vst1.16		{d18}, [lr], r6			@4'
		vsub.s32	q5, q5, q11				@11'
		
		tx_o16_5	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[5]
		vadd.s32	q9, q4, q11
		vqrshrn.s32	d18, q9, #7
		vst1.16		{d18}, [lr], r6			@5'
		vsub.s32	q4, q4, q11				@10'
		
		tx_o16_6	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[6]
		vadd.s32	q9, q3, q11
		vqrshrn.s32	d18, q9, #7
		vst1.16		{d18}, [lr], r6			@6'
		vsub.s32	q3, q3, q11				@9'
		
		tx_o16_7	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[7]
		vadd.s32	q9, q2, q11
		vqrshrn.s32	d18, q9, #7
		vst1.16		{d18}, [lr], r6			@7'
		vsub.s32	q2, q2, q11				@8'
		vqrshrn.s32	d4, q2, #7
		vqrshrn.s32	d5, q3, #7
		vqrshrn.s32	d6, q4, #7
		vqrshrn.s32	d7, q5, #7
		vqrshrn.s32	d8, q8, #7
		vqrshrn.s32	d9, q7, #7
		vqrshrn.s32	d10, q6, #7
		vqrshrn.s32	d11, q10, #7
		vst1.16		{d4}, [lr], r6
		vst1.16		{d5}, [lr], r6
		vst1.16		{d6}, [lr], r6
		vst1.16		{d7}, [lr], r6
		vst1.16		{d8}, [lr], r6
		vst1.16		{d9}, [lr], r6
		vst1.16		{d10}, [lr], r6
		vst1.16		{d11}, [lr], r6
		
.endm
.macro tx16x4_h
		mov		lr, r1
		vld1.16		{q12, q13}, [lr]!
		vld1.16		{q2,  q3},  [lr]!
		vld1.16		{q14, q15}, [lr]!
		vld1.16		{q4,  q5},  [lr]!
		vtrn.16		q12, q2
		vtrn.16		q13, q3
		vtrn.16		q14, q4
		vtrn.16		q15, q5
		vtrn.32		q12, q14				@d24:src0 d25:src4    d28:src2 d29:src6
		vtrn.32		q13, q15				@d26:src8 d27:src12   d30:src10 d31:src14
		
		tx8x4_set	d24, d28, d25, d29, d26, d30, d27, d31		@q10 6 7 8 2 3 4 5
			
		vld1.16		{q14, q15}, [r1]!
		vld1.16		{q12, q13}, [r1]!
		vld1.16		{q9},  [r1]!
		vld1.16		{q11},  [r1]!
		vtrn.16		q14, q12
		vtrn.16		q15, q13
		vld1.16		{q14, q15}, [r1]!
		vtrn.16		q9, q14
		vtrn.16		q11, q15
		vtrn.32		q12, q14				@d24:src1 d25:src5    d28:src3 d29:src7
		vtrn.32		q13, q15				@d26:src9 d27:src13   d30:src11 d31:src15
		
		tx_o16_0	q11, d24, d28, d25, d29, d26, d30, d27, d31, d2, d3		@o_16[0]
		vadd.s32	q9,  q10, q11
		vsub.s32	q10, q10, q11
		vqrshrn.s32	d21, q10, #12			@15'
		vqrshrn.s32	d20, q9, #12			@0'
		
		tx_o16_1	q11, d24, d28, d25, d29, d26, d30, d27, d31, d2, d3		@o_16[1]
		vadd.s32	q9, q6, q11
		vsub.s32	q6, q6, q11	
		vqrshrn.s32	d13, q6, #12			@14'
		vqrshrn.s32	d12, q9, #12			@1'
		
		tx_o16_2	q11, d24, d28, d25, d29, d26, d30, d27, d31, d2, d3		@o_16[2]
		vadd.s32	q9, q7, q11
		vsub.s32	q7, q7, q11	
		vqrshrn.s32	d15, q7, #12			@13'
		vqrshrn.s32	d14, q9, #12			@2'
		
		tx_o16_3	q11, d24, d28, d25, d29, d26, d30, d27, d31, d2, d3		@o_16[3]
		vadd.s32	q9, q8, q11
		vsub.s32	q8, q8, q11
		vqrshrn.s32	d17, q8, #12			@12'
		vqrshrn.s32	d16, q9, #12			@3'
		
		tx_o16_4	q11, d24, d28, d25, d29, d26, d30, d27, d31, d2, d3		@o_16[4]
		vadd.s32	q9, q5, q11
		vsub.s32	q5, q5, q11	
		vqrshrn.s32	d11, q5, #12			@11'
		vqrshrn.s32	d10, q9, #12			@4'
		
		tx_o16_5	q11, d24, d28, d25, d29, d26, d30, d27, d31, d2, d3		@o_16[5]
		vadd.s32	q9, q4, q11
		vsub.s32	q4, q4, q11
		vqrshrn.s32	d9, q4, #12				@10'
		vqrshrn.s32	d8, q9, #12				@5'
		
		tx_o16_6	q11, d24, d28, d25, d29, d26, d30, d27, d31, d2, d3		@o_16[6]
		vadd.s32	q9, q3, q11
		vsub.s32	q3, q3, q11
		vqrshrn.s32	d7, q3, #12				@9'
		vqrshrn.s32	d6, q9, #12				@6'
		
		tx_o16_7	q11, d24, d28, d25, d29, d26, d30, d27, d31, d2, d3		@o_16[7]
		vadd.s32	q9, q2, q11
		vsub.s32	q2, q2, q11
		vqrshrn.s32	d5, q2, #12				@8'
		vqrshrn.s32	d4, q9, #12				@7'
		
		vswp		d21, d10
		vswp		d13, d8
		vswp		d15, d6
		vswp		d17, d4
		vswp		d4, d5
		vswp		d6, d7
		vswp		d8, d9
		vswp		d10, d11

		vtrn.16		q10, q6
		vtrn.16		q7, q8
		vtrn.32		q10, q7
		vtrn.32		q6, q8
		vtrn.16		q2, q3
		vtrn.16		q4, q5
		vtrn.32		q2, q4
		vtrn.32		q3, q5
		
		mov		lr, r0
		vld1.8		{q12}, [lr], r2
		vld1.8		{q13}, [lr], r2
		vld1.8		{q14}, [lr], r2
		vld1.8		{q15}, [lr], r2
		
		vaddw.u8	q10, q10, d24
		vaddw.u8	q2, q2, d25
		vqmovun.s16	d24,q10
		vqmovun.s16	d25,q2
		
		vaddw.u8	q6, q6, d26
		vaddw.u8	q3, q3, d27
		vqmovun.s16	d26,q6
		vqmovun.s16	d27,q3
		vaddw.u8	q7, q7, d28
		vaddw.u8	q4, q4, d29
		vqmovun.s16	d28,q7
		vqmovun.s16	d29,q4
		vaddw.u8	q8, q8, d30
		vaddw.u8	q5, q5, d31
		vqmovun.s16	d30,q8
		vqmovun.s16	d31,q5
		
		vst1.8		{q12}, [r0], r2
		vst1.8		{q13}, [r0], r2
		vst1.8		{q14}, [r0], r2
		vst1.8		{q15}, [r0], r2
		
.endm

#====================================================================
#void transform_16x16_add(
#r0:    uint8_t *_dst, 
#r1:	int16_t *coeffs, 
#r2:	ptrdiff_t _stride, 
#r3:	int col_limit);
#====================================================================
function ff_hevc_transform_16x16_add_neon, export=1
	stmfd	sp!, {r4 - r6, lr}
		mov		r12, #64
		adr		r3, Tx4_idx
		lsr		r6, r12, #1					@r6: 32
		
		vld1.16		{q0, q1}, [r3]			@d0:83 36 0 0   d1:89 75 50 18
											@q1: 90, 87, 80, 70, 57, 43, 25, 9
											
		tx16x4_v 
		add		r1, r1, #8
		tx16x4_v 
		add		r1, r1, #8
		tx16x4_v 
		add		r1, r1, #8
		tx16x4_v 
#---------------------------------------------------------------------		
		sub		r1, r1, #24
		
		tx16x4_h
#		add		r1, r1, #128
		tx16x4_h
#		add		r1, r1, #128
		tx16x4_h
#		add		r1, r1, #128
		tx16x4_h
	ldmfd	sp!, {r4 - r6, pc}
endfunc
	
.macro tx_o32_0	dq0, idx_d0, idx_d1 idx_d2, idx_d3
		vmull.s16	\dq0, d16, \idx_d0[0]			@ 90*src1
		vmlal.s16	\dq0, d17, \idx_d0[1]			@ 90*src3
		vmlal.s16	\dq0, d18, \idx_d0[2]			@ 88*src5
		vmlal.s16	\dq0, d19, \idx_d0[3]			@ 85*src7
		vmlal.s16	\dq0, d20, \idx_d1[0]			@ 82*src9
		vmlal.s16	\dq0, d21, \idx_d1[1]			@ 78*src11
		vmlal.s16	\dq0, d22, \idx_d1[2]			@ 73*src13
		vmlal.s16	\dq0, d23, \idx_d1[3]			@ 67*src15
		vmlal.s16	\dq0, d24, \idx_d2[0]			@ 61*src17
		vmlal.s16	\dq0, d25, \idx_d2[1]			@ 54*src19
		vmlal.s16	\dq0, d26, \idx_d2[2]			@ 46*src21
		vmlal.s16	\dq0, d27, \idx_d2[3]			@ 38*src23
		vmlal.s16	\dq0, d28, \idx_d3[0]			@ 31*src25
		vmlal.s16	\dq0, d29, \idx_d3[1]			@ 22*src27
		vmlal.s16	\dq0, d30, \idx_d3[2]			@ 13*src29
		vmlal.s16	\dq0, d31, \idx_d3[3]			@  4*src31			@o_32[0]
.endm
.macro tx_o32_1	dq0, idx_d0, idx_d1 idx_d2, idx_d3
		vmull.s16	\dq0, d16, \idx_d0[1]			@ 90*src1
		vmlal.s16	\dq0, d17, \idx_d1[0]			@ 82*src3
		vmlal.s16	\dq0, d18, \idx_d1[3]			@ 67*src5
		vmlal.s16	\dq0, d19, \idx_d2[2]			@ 46*src7
		vmlal.s16	\dq0, d20, \idx_d3[1]			@ 22*src9
		vmlsl.s16	\dq0, d21, \idx_d3[3]			@- 4*src11
		vmlsl.s16	\dq0, d22, \idx_d3[0]			@-31*src13
		vmlsl.s16	\dq0, d23, \idx_d2[1]			@-54*src15
		vmlsl.s16	\dq0, d24, \idx_d1[2]			@-73*src17
		vmlsl.s16	\dq0, d25, \idx_d0[3]			@-85*src19
		vmlsl.s16	\dq0, d26, \idx_d0[0]			@-90*src21
		vmlsl.s16	\dq0, d27, \idx_d0[2]			@-88*src23
		vmlsl.s16	\dq0, d28, \idx_d1[1]			@-78*src25
		vmlsl.s16	\dq0, d29, \idx_d2[0]			@-61*src27
		vmlsl.s16	\dq0, d30, \idx_d2[3]			@-38*src29
		vmlsl.s16	\dq0, d31, \idx_d3[2]			@-13*src31			@o_32[1]
.endm
.macro tx_o32_2	dq0, idx_d0, idx_d1 idx_d2, idx_d3
		vmull.s16	\dq0, d16, \idx_d0[2]			@ 88*src1
		vmlal.s16	\dq0, d17, \idx_d1[3]			@ 67*src3
		vmlal.s16	\dq0, d18, \idx_d3[0]			@ 31*src5
		vmlsl.s16	\dq0, d19, \idx_d3[2]			@-13*src7
		vmlsl.s16	\dq0, d20, \idx_d2[1]			@-54*src9
		vmlsl.s16	\dq0, d21, \idx_d1[0]			@-82*src11
		vmlsl.s16	\dq0, d22, \idx_d0[0]			@-90*src13
		vmlsl.s16	\dq0, d23, \idx_d1[1]			@-78*src15
		vmlsl.s16	\dq0, d24, \idx_d2[2]			@-46*src17
		vmlsl.s16	\dq0, d25, \idx_d3[3]			@- 4*src19
		vmlal.s16	\dq0, d26, \idx_d2[3]			@ 38*src21
		vmlal.s16	\dq0, d27, \idx_d1[2]			@ 73*src23
		vmlal.s16	\dq0, d28, \idx_d0[1]			@ 90*src25
		vmlal.s16	\dq0, d29, \idx_d0[3]			@ 85*src27
		vmlal.s16	\dq0, d30, \idx_d2[0]			@ 61*src29
		vmlal.s16	\dq0, d31, \idx_d3[1]			@ 22*src31			@o_32[2]
.endm
.macro tx_o32_3	dq0, idx_d0, idx_d1 idx_d2, idx_d3
		vmull.s16	\dq0, d16, \idx_d0[3]			@ 85*src1
		vmlal.s16	\dq0, d17, \idx_d2[2]			@ 46*src3
		vmlsl.s16	\dq0, d18, \idx_d3[2]			@-13*src5
		vmlsl.s16	\dq0, d19, \idx_d1[3]			@-67*src7
		vmlsl.s16	\dq0, d20, \idx_d0[0]			@-90*src9
		vmlsl.s16	\dq0, d21, \idx_d1[2]			@-73*src11
		vmlsl.s16	\dq0, d22, \idx_d3[1]			@-22*src13
		vmlal.s16	\dq0, d23, \idx_d2[3]			@ 38*src15
		vmlal.s16	\dq0, d24, \idx_d1[0]			@ 82*src17
		vmlal.s16	\dq0, d25, \idx_d0[2]			@ 88*src19
		vmlal.s16	\dq0, d26, \idx_d2[1]			@ 54*src21
		vmlsl.s16	\dq0, d27, \idx_d3[3]			@- 4*src23
		vmlsl.s16	\dq0, d28, \idx_d2[0]			@-61*src25
		vmlsl.s16	\dq0, d29, \idx_d0[1]			@-90*src27
		vmlsl.s16	\dq0, d30, \idx_d1[1]			@-78*src29
		vmlsl.s16	\dq0, d31, \idx_d3[0]			@-31*src31			@o_32[3]
.endm
.macro tx_o32_4	dq0, idx_d0, idx_d1 idx_d2, idx_d3
		vmull.s16	\dq0, d16, \idx_d1[0]			@ 82*src1
		vmlal.s16	\dq0, d17, \idx_d3[1]			@ 22*src3
		vmlsl.s16	\dq0, d18, \idx_d2[1]			@-54*src5
		vmlsl.s16	\dq0, d19, \idx_d0[0]			@-90*src7
		vmlsl.s16	\dq0, d20, \idx_d2[0]			@-61*src9
		vmlal.s16	\dq0, d21, \idx_d3[2]			@ 13*src11
		vmlal.s16	\dq0, d22, \idx_d1[1]			@ 78*src13
		vmlal.s16	\dq0, d23, \idx_d0[3]			@ 85*src15
		vmlal.s16	\dq0, d24, \idx_d3[0]			@ 31*src17
		vmlsl.s16	\dq0, d25, \idx_d2[2]			@-46*src19
		vmlsl.s16	\dq0, d26, \idx_d0[1]			@-90*src21
		vmlsl.s16	\dq0, d27, \idx_d1[3]			@-67*src23
		vmlal.s16	\dq0, d28, \idx_d3[3]			@  4*src25
		vmlal.s16	\dq0, d29, \idx_d1[2]			@ 73*src27
		vmlal.s16	\dq0, d30, \idx_d0[2]			@ 88*src29
		vmlal.s16	\dq0, d31, \idx_d2[3]			@ 38*src31			@o_32[4]
.endm
.macro tx_o32_5	dq0, idx_d0, idx_d1 idx_d2, idx_d3
		vmull.s16	\dq0, d16, \idx_d1[1]			@ 78*src1
		vmlsl.s16	\dq0, d17, \idx_d3[3]			@- 4*src3
		vmlsl.s16	\dq0, d18, \idx_d1[0]			@-82*src5
		vmlsl.s16	\dq0, d19, \idx_d1[2]			@-73*src7
		vmlal.s16	\dq0, d20, \idx_d3[2]			@ 13*src9
		vmlal.s16	\dq0, d21, \idx_d0[3]			@ 85*src11
		vmlal.s16	\dq0, d22, \idx_d1[3]			@ 67*src13
		vmlsl.s16	\dq0, d23, \idx_d3[1]			@-22*src15
		vmlsl.s16	\dq0, d24, \idx_d0[2]			@-88*src17
		vmlsl.s16	\dq0, d25, \idx_d2[0]			@-61*src19
		vmlal.s16	\dq0, d26, \idx_d3[0]			@ 31*src21
		vmlal.s16	\dq0, d27, \idx_d0[0]			@ 90*src23
		vmlal.s16	\dq0, d28, \idx_d2[1]			@ 54*src25
		vmlsl.s16	\dq0, d29, \idx_d2[3]			@-38*src27
		vmlsl.s16	\dq0, d30, \idx_d0[1]			@-90*src29
		vmlsl.s16	\dq0, d31, \idx_d2[2]			@-46*src31			@o_32[5]
.endm
.macro tx_o32_6	dq0, idx_d0, idx_d1 idx_d2, idx_d3
		vmull.s16	\dq0, d16, \idx_d1[2]			@ 73*src1
		vmlsl.s16	\dq0, d17, \idx_d3[0]			@-31*src3
		vmlsl.s16	\dq0, d18, \idx_d0[0]			@-90*src5
		vmlsl.s16	\dq0, d19, \idx_d3[1]			@-22*src7
		vmlal.s16	\dq0, d20, \idx_d1[1]			@ 78*src9
		vmlal.s16	\dq0, d21, \idx_d1[3]			@ 67*src11
		vmlsl.s16	\dq0, d22, \idx_d2[3]			@-38*src13
		vmlsl.s16	\dq0, d23, \idx_d0[1]			@-90*src15
		vmlsl.s16	\dq0, d24, \idx_d3[2]			@-13*src17
		vmlal.s16	\dq0, d25, \idx_d1[0]			@ 82*src19
		vmlal.s16	\dq0, d26, \idx_d2[0]			@ 61*src21
		vmlsl.s16	\dq0, d27, \idx_d2[2]			@-46*src23
		vmlsl.s16	\dq0, d28, \idx_d0[2]			@-88*src25
		vmlsl.s16	\dq0, d29, \idx_d3[3]			@- 4*src27
		vmlal.s16	\dq0, d30, \idx_d0[3]			@ 85*src29
		vmlal.s16	\dq0, d31, \idx_d2[1]			@ 54*src31			@o_32[6]
.endm
.macro tx_o32_7	dq0, idx_d0, idx_d1 idx_d2, idx_d3
		vmull.s16	\dq0, d16, \idx_d1[3]			@ 67*src1
		vmlsl.s16	\dq0, d17, \idx_d2[1]			@-54*src3
		vmlsl.s16	\dq0, d18, \idx_d1[1]			@-78*src5
		vmlal.s16	\dq0, d19, \idx_d2[3]			@ 38*src7
		vmlal.s16	\dq0, d20, \idx_d0[3]			@ 85*src9
		vmlsl.s16	\dq0, d21, \idx_d3[1]			@-22*src11
		vmlsl.s16	\dq0, d22, \idx_d0[0]			@-90*src13
		vmlal.s16	\dq0, d23, \idx_d3[3]			@  4*src15
		vmlal.s16	\dq0, d24, \idx_d0[1]			@ 90*src17
		vmlal.s16	\dq0, d25, \idx_d3[2]			@ 13*src19
		vmlsl.s16	\dq0, d26, \idx_d0[2]			@-88*src21
		vmlsl.s16	\dq0, d27, \idx_d3[0]			@-31*src23
		vmlal.s16	\dq0, d28, \idx_d1[0]			@ 82*src25
		vmlal.s16	\dq0, d29, \idx_d2[2]			@ 46*src27
		vmlsl.s16	\dq0, d30, \idx_d1[2]			@-73*src29
		vmlsl.s16	\dq0, d31, \idx_d2[0]			@-61*src31			@o_32[7]
.endm
.macro tx_o32_8	dq0, idx_d0, idx_d1 idx_d2, idx_d3
		vmull.s16	\dq0, d16, \idx_d2[0]			@ 61*src1
		vmlsl.s16	\dq0, d17, \idx_d1[2]			@-73*src3
		vmlsl.s16	\dq0, d18, \idx_d2[2]			@-46*src5
		vmlal.s16	\dq0, d19, \idx_d1[0]			@ 82*src7
		vmlal.s16	\dq0, d20, \idx_d3[0]			@ 31*src9
		vmlsl.s16	\dq0, d21, \idx_d0[2]			@-88*src11
		vmlsl.s16	\dq0, d22, \idx_d3[2]			@-13*src13
		vmlal.s16	\dq0, d23, \idx_d0[1]			@ 90*src15
		vmlsl.s16	\dq0, d24, \idx_d3[3]			@- 4*src17
		vmlsl.s16	\dq0, d25, \idx_d0[0]			@-90*src19
		vmlal.s16	\dq0, d26, \idx_d3[1]			@ 22*src21
		vmlal.s16	\dq0, d27, \idx_d0[3]			@ 85*src23
		vmlsl.s16	\dq0, d28, \idx_d2[3]			@-38*src25
		vmlsl.s16	\dq0, d29, \idx_d1[1]			@-78*src27
		vmlal.s16	\dq0, d30, \idx_d2[1]			@ 54*src29
		vmlal.s16	\dq0, d31, \idx_d1[3]			@ 67*src31			@o_32[8]
.endm
.macro tx_o32_9	dq0, idx_d0, idx_d1 idx_d2, idx_d3
		vmull.s16	\dq0, d16, \idx_d2[1]			@ 54*src1
		vmlsl.s16	\dq0, d17, \idx_d0[3]			@-85*src3
		vmlsl.s16	\dq0, d18, \idx_d3[3]			@- 4*src5
		vmlal.s16	\dq0, d19, \idx_d0[2]			@ 88*src7
		vmlsl.s16	\dq0, d20, \idx_d2[2]			@-46*src9
		vmlsl.s16	\dq0, d21, \idx_d2[0]			@-61*src11
		vmlal.s16	\dq0, d22, \idx_d1[0]			@ 82*src13
		vmlal.s16	\dq0, d23, \idx_d3[2]			@ 13*src15
		vmlsl.s16	\dq0, d24, \idx_d0[1]			@-90*src17
		vmlal.s16	\dq0, d25, \idx_d2[3]			@ 38*src19
		vmlal.s16	\dq0, d26, \idx_d1[3]			@ 67*src21
		vmlsl.s16	\dq0, d27, \idx_d1[1]			@-78*src23
		vmlsl.s16	\dq0, d28, \idx_d3[1]			@-22*src25
		vmlal.s16	\dq0, d29, \idx_d0[0]			@ 90*src27
		vmlsl.s16	\dq0, d30, \idx_d3[0]			@-31*src29
		vmlsl.s16	\dq0, d31, \idx_d1[2]			@-73*src31			@o_32[9]
.endm
.macro tx_o32_10	dq0, idx_d0, idx_d1 idx_d2, idx_d3
		vmull.s16	\dq0, d16, \idx_d2[2]			@ 46*src1
		vmlsl.s16	\dq0, d17, \idx_d0[1]			@-90*src3
		vmlal.s16	\dq0, d18, \idx_d2[3]			@ 38*src5
		vmlal.s16	\dq0, d19, \idx_d2[1]			@ 54*src7
		vmlsl.s16	\dq0, d20, \idx_d0[0]			@-90*src9
		vmlal.s16	\dq0, d21, \idx_d3[0]			@ 31*src11
		vmlal.s16	\dq0, d22, \idx_d2[0]			@ 61*src13
		vmlsl.s16	\dq0, d23, \idx_d0[2]			@-88*src15
		vmlal.s16	\dq0, d24, \idx_d3[1]			@ 22*src17
		vmlal.s16	\dq0, d25, \idx_d1[3]			@ 67*src19
		vmlsl.s16	\dq0, d26, \idx_d0[3]			@-85*src21
		vmlal.s16	\dq0, d27, \idx_d3[2]			@ 13*src23
		vmlal.s16	\dq0, d28, \idx_d1[2]			@ 73*src25
		vmlsl.s16	\dq0, d29, \idx_d1[0]			@-82*src27
		vmlal.s16	\dq0, d30, \idx_d3[3]			@  4*src29
		vmlal.s16	\dq0, d31, \idx_d1[1]			@ 78*src31			@o_32[10]
.endm
.macro tx_o32_11	dq0, idx_d0, idx_d1 idx_d2, idx_d3
		vmull.s16	\dq0, d16, \idx_d2[3]			@ 38*src1
		vmlsl.s16	\dq0, d17, \idx_d0[2]			@-88*src3
		vmlal.s16	\dq0, d18, \idx_d1[2]			@ 73*src5
		vmlsl.s16	\dq0, d19, \idx_d3[3]			@- 4*src7
		vmlsl.s16	\dq0, d20, \idx_d1[3]			@-67*src9
		vmlal.s16	\dq0, d21, \idx_d0[1]			@ 90*src11
		vmlsl.s16	\dq0, d22, \idx_d2[2]			@-46*src13
		vmlsl.s16	\dq0, d23, \idx_d3[0]			@-31*src15
		vmlal.s16	\dq0, d24, \idx_d0[3]			@ 85*src17
		vmlsl.s16	\dq0, d25, \idx_d1[1]			@-78*src19
		vmlal.s16	\dq0, d26, \idx_d3[2]			@ 13*src21
		vmlal.s16	\dq0, d27, \idx_d2[0]			@ 61*src23
		vmlsl.s16	\dq0, d28, \idx_d0[0]			@-90*src25
		vmlal.s16	\dq0, d29, \idx_d2[1]			@ 54*src27
		vmlal.s16	\dq0, d30, \idx_d3[1]			@ 22*src29
		vmlsl.s16	\dq0, d31, \idx_d1[0]			@-82*src31			@o_32[11]
.endm
.macro tx_o32_12	dq0, idx_d0, idx_d1 idx_d2, idx_d3
		vmull.s16	\dq0, d16, \idx_d3[0]			@ 31*src1
		vmlsl.s16	\dq0, d17, \idx_d1[1]			@-78*src3
		vmlal.s16	\dq0, d18, \idx_d0[1]			@ 90*src5
		vmlsl.s16	\dq0, d19, \idx_d2[0]			@-61*src7
		vmlal.s16	\dq0, d20, \idx_d3[3]			@  4*src9
		vmlal.s16	\dq0, d21, \idx_d2[1]			@ 54*src11
		vmlsl.s16	\dq0, d22, \idx_d0[2]			@-88*src13
		vmlal.s16	\dq0, d23, \idx_d1[0]			@ 82*src15
		vmlsl.s16	\dq0, d24, \idx_d2[3]			@-38*src17
		vmlsl.s16	\dq0, d25, \idx_d3[1]			@-22*src19
		vmlal.s16	\dq0, d26, \idx_d1[2]			@ 73*src21
		vmlsl.s16	\dq0, d27, \idx_d0[0]			@-90*src23
		vmlal.s16	\dq0, d28, \idx_d1[3]			@ 67*src25
		vmlsl.s16	\dq0, d29, \idx_d3[2]			@-13*src27
		vmlsl.s16	\dq0, d30, \idx_d2[2]			@-46*src29
		vmlal.s16	\dq0, d31, \idx_d0[3]			@ 85*src31			@o_32[12]
.endm
.macro tx_o32_13	dq0, idx_d0, idx_d1 idx_d2, idx_d3
		vmull.s16	\dq0, d16, \idx_d3[1]			@ 22*src1
		vmlsl.s16	\dq0, d17, \idx_d2[0]			@-61*src3
		vmlal.s16	\dq0, d18, \idx_d0[3]			@ 85*src5
		vmlsl.s16	\dq0, d19, \idx_d0[0]			@-90*src7
		vmlal.s16	\dq0, d20, \idx_d1[2]			@ 73*src9
		vmlsl.s16	\dq0, d21, \idx_d2[3]			@-38*src11
		vmlsl.s16	\dq0, d22, \idx_d3[3]			@- 4*src13
		vmlal.s16	\dq0, d23, \idx_d2[2]			@ 46*src15
		vmlsl.s16	\dq0, d24, \idx_d1[1]			@-78*src17
		vmlal.s16	\dq0, d25, \idx_d0[1]			@ 90*src19
		vmlsl.s16	\dq0, d26, \idx_d1[0]			@-82*src21
		vmlal.s16	\dq0, d27, \idx_d2[1]			@ 54*src23
		vmlsl.s16	\dq0, d28, \idx_d3[2]			@-13*src25
		vmlsl.s16	\dq0, d29, \idx_d3[0]			@-31*src27
		vmlal.s16	\dq0, d30, \idx_d1[3]			@ 67*src29
		vmlsl.s16	\dq0, d31, \idx_d0[2]			@-88*src31			@o_32[13]
.endm
.macro tx_o32_14	dq0, idx_d0, idx_d1 idx_d2, idx_d3
		vmull.s16	\dq0, d16, \idx_d3[2]			@ 13*src1
		vmlsl.s16	\dq0, d17, \idx_d2[3]			@-38*src3
		vmlal.s16	\dq0, d18, \idx_d2[0]			@ 61*src5
		vmlsl.s16	\dq0, d19, \idx_d1[1]			@-78*src7
		vmlal.s16	\dq0, d20, \idx_d0[2]			@ 88*src9
		vmlsl.s16	\dq0, d21, \idx_d0[0]			@-90*src11
		vmlal.s16	\dq0, d22, \idx_d0[3]			@ 85*src13
		vmlsl.s16	\dq0, d23, \idx_d1[2]			@-73*src15
		vmlal.s16	\dq0, d24, \idx_d2[1]			@ 54*src17
		vmlsl.s16	\dq0, d25, \idx_d3[0]			@-31*src19
		vmlal.s16	\dq0, d26, \idx_d3[3]			@  4*src21
		vmlal.s16	\dq0, d27, \idx_d3[1]			@ 22*src23
		vmlsl.s16	\dq0, d28, \idx_d2[2]			@-46*src25
		vmlal.s16	\dq0, d29, \idx_d1[3]			@ 67*src27
		vmlsl.s16	\dq0, d30, \idx_d1[0]			@-82*src29
		vmlal.s16	\dq0, d31, \idx_d0[1]			@ 90*src31			@o_32[14]
.endm
.macro tx_o32_15	dq0, idx_d0, idx_d1 idx_d2, idx_d3
		vmull.s16	\dq0, d16, \idx_d3[3]			@  4*src1
		vmlsl.s16	\dq0, d17, \idx_d3[2]			@-13*src3
		vmlal.s16	\dq0, d18, \idx_d3[1]			@ 22*src5
		vmlsl.s16	\dq0, d19, \idx_d3[0]			@-31*src7
		vmlal.s16	\dq0, d20, \idx_d2[3]			@ 38*src9
		vmlsl.s16	\dq0, d21, \idx_d2[2]			@-46*src11
		vmlal.s16	\dq0, d22, \idx_d2[1]			@ 54*src13
		vmlsl.s16	\dq0, d23, \idx_d2[0]			@-61*src15
		vmlal.s16	\dq0, d24, \idx_d1[3]			@ 67*src17
		vmlsl.s16	\dq0, d25, \idx_d1[2]			@-73*src19
		vmlal.s16	\dq0, d26, \idx_d1[1]			@ 78*src21
		vmlsl.s16	\dq0, d27, \idx_d1[0]			@-82*src23
		vmlal.s16	\dq0, d28, \idx_d0[3]			@ 85*src25
		vmlsl.s16	\dq0, d29, \idx_d0[2]			@-88*src27
		vmlal.s16	\dq0, d30, \idx_d0[1]			@ 90*src29
		vmlsl.s16	\dq0, d31, \idx_d0[0]			@-90*src31			@o_32[15]
.endm

.macro tx16x4_v_set
											
		mov		lr, r1
		vld1.16		{d24}, [lr], r12		@src0
		vld1.16		{d25}, [lr], r12		@src2
		vld1.16		{d26}, [lr], r12		@src4
		vld1.16		{d27}, [lr], r12		@src6
		vld1.16		{d28}, [lr], r12		@src8
		vld1.16		{d29}, [lr], r12		@src10
		vld1.16		{d30}, [lr], r12		@src12
		vld1.16		{d31}, [lr], r12		@src14
		
		tx8x4_set	d24, d25, d26, d27, d28, d29, d30, d31
		
		add		lr, r1, r6
		vld1.16		{d24}, [lr], r12		@src1
		vld1.16		{d25}, [lr], r12		@src3
		vld1.16		{d26}, [lr], r12		@src5
		vld1.16		{d27}, [lr], r12		@src7
		vld1.16		{d28}, [lr], r12		@src9
		vld1.16		{d29}, [lr], r12		@src11
		vld1.16		{d30}, [lr], r12		@src13
		vld1.16		{d31}, [lr], r12		@src15

		tx_o16_0	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[0]
		vsub.s32	q9, q10, q11			@e_32[15]
		vpush		{q9}
		vadd.s32	q10,  q10, q11			@e_32[0]
		
		tx_o16_1	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[1]
		vsub.s32	q9, q6, q11				@e_32[14]
		vpush		{q9}
		vadd.s32	q6, q6, q11				@e_32[1]
		
		tx_o16_2	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[2]
		vsub.s32	q9, q7, q11				@e_32[13]
		vpush		{q9}
		vadd.s32	q7, q7, q11				@e_32[2]
		
		tx_o16_3	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[3]
		vsub.s32	q9, q8, q11				@e_32[12]
		vpush		{q9}
		vadd.s32	q8, q8, q11				@e_32[3]
		
		tx_o16_4	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[4]
		vsub.s32	q9, q5, q11				@e_32[11]
		vpush		{q9}
		vadd.s32	q5, q5, q11				@e_32[4]
		
		tx_o16_5	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[5]
		vsub.s32	q9, q4, q11				@e_32[10]
		vpush		{q9}
		vadd.s32	q4, q4, q11				@e_32[5]
		
		tx_o16_6	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[6]
		vsub.s32	q9, q3, q11				@e_32[9]
		vpush		{q9}
		vadd.s32	q3, q3, q11				@e_32[6]
		
		tx_o16_7	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[7]
		vsub.s32	q9, q2, q11				@e_32[8]
		vpush		{q9}
		vadd.s32	q2, q2, q11				@e_32[7]
#		vpush		{q2 - q5}
		vpush		{q2}
		vpush		{q3}
		vpush		{q4}
		vpush		{q5}
		vpush		{q6-q8}
		vpush		{q10}
.endm
.macro	tx31x1	idx, shift
		vpop		{q2}					@e_32[0]
		tx_o32_\idx	q3, d0, d1, d2, d3
		vadd.s32	q4, q2, q3
		vsub.s32	q5, q2, q3
		vqrshrn.s32	d8, q4, \shift
		vqrshrn.s32	d10, q5, \shift
		vst1.16		{d8}, [lr], r4			@0'
		vst1.16		{d10}, [r7], r5			@31'
.endm
.macro	tx32x4_v

		vld1.16		{q0, q1}, [r3]!			@d0:83 36 0 0   d1:89 75 50 18
											@q1: 90, 87, 80, 70, 57, 43, 25, 9
		tx16x4_v_set 
		vld1.16		{q0, q1}, [r3]			@q0:90 90 88 85 82 78 73 67
											@q1:61 54 46 38 31 22 13  4
		sub		r3, r3, #32					@r3:Tx4_idx
		
		add		lr, r1, r4					@lr:src+stride
		vld1.16		{d16}, [lr], r6 		@src1
		vld1.16		{d17}, [lr], r6 		@src3
		vld1.16		{d18}, [lr], r6 		@src5
		vld1.16		{d19}, [lr], r6 		@src7
		vld1.16		{d20}, [lr], r6 		@src9
		vld1.16		{d21}, [lr], r6 		@src11
		vld1.16		{d22}, [lr], r6 		@src13
		vld1.16		{d23}, [lr], r6 		@src15
		vld1.16		{d24}, [lr], r6 		@src17
		vld1.16		{d25}, [lr], r6 		@src19
		vld1.16		{d26}, [lr], r6 		@src21
		vld1.16		{d27}, [lr], r6 		@src23
		vld1.16		{d28}, [lr], r6 		@src25
		vld1.16		{d29}, [lr], r6 		@src27
		vld1.16		{d30}, [lr], r6 		@src29
		vld1.16		{d31}, [lr], r6 		@src31
		mov		lr, r1
		add		r7, r1, #1984
		tx31x1		0, #7
		tx31x1		1, #7
		tx31x1		2, #7
		tx31x1		3, #7
		tx31x1		4, #7
		tx31x1		5, #7
		tx31x1		6, #7
		tx31x1		7, #7
		tx31x1		8, #7
		tx31x1		9, #7
		tx31x1		10, #7
		tx31x1		11, #7
		tx31x1		12, #7
		tx31x1		13, #7
		tx31x1		14, #7
		tx31x1		15, #7
.endm

.macro tx16x4_h_set
		vld1.16		{q12, q13}, [r1]!
		vld1.16		{q14, q15}, [r1]!
		mov		lr, r1						@r1: +=64
		vld1.16		{q0, q1}, [lr]!
		vld1.16		{q2, q3}, [lr]!
		vld1.16		{q8, q9}, [lr]!
		vld1.16		{q10, q11}, [lr]!
		vld1.16		{q4, q5}, [lr]!
		vld1.16		{q6, q7}, [lr]!
		vtrn.16		q12, q0
		vtrn.16		q13, q1
		vtrn.16		q14, q2
		vtrn.16		q15, q3
		vtrn.16		q8,  q4
		vtrn.16		q9,  q5
		vtrn.16		q10, q6
		vtrn.16		q11, q7
		vtrn.32		q12, q8				@d24:src0  d25:src4		@d16:src2  d17:src6
		vtrn.32		q13, q9				@d26:src8  d27:src12	@d18:src10 d19:src14
		vtrn.32		q14, q10			@d28:src16 d29:src20	@d20:src18 d21:src22
		vtrn.32		q15, q11			@d30:src24 d31:src28	@d22:src26 d23:src30
		vtrn.32		q0, q4				@d0:src1   d1:src5		@ d8:src3    d9:src7
		vtrn.32		q1, q5				@d2:src9   d3:src13		@d10:src11  d11:src15
		vtrn.32		q2, q6				@d4:src17  d5:src21		@d12:src19  d13:src23
		vtrn.32		q3, q7				@d6:src25  d7:src29		@d14:src27  d15:src31
		vswp		d1, d8
		vswp		d3, d10
		vswp		d5, d12
		vswp		d7, d14
		
		mov		lr, r1					@->src0+64
		vst1.16		{q8, q9}, [lr]!		@src  2  6 10 14
		vst1.16		{q10, q11}, [lr]!	@src 18 22 26 30
		vst1.16		{q0}, [lr]!			@src  1  3
		vst1.16		{q4}, [lr]!			@src  5  7
		vst1.16		{q1}, [lr]!			@src  9 11
		vst1.16		{q5}, [lr]!			@src 13 15
		vst1.16		{q2}, [lr]!			@src 17 19
		vst1.16		{q6}, [lr]!			@src 21 23
		vst1.16		{q3}, [lr]!			@src 25 27
		vst1.16		{q7}, [lr]!			@src 29 31
		
		vld1.16		{q0, q1}, [r3]!			@d0:83 36 0 0   d1:89 75 50 18
											@q1: 90, 87, 80, 70, 57, 43, 25, 9
		tx8x4_set	d24, d25, d26, d27, d28, d29, d30, d31		@q10 6 7 8 5 4 3 2
		
		vld1.16		{q12, q13}, [r1]!		@src  2  6 10 14
		vld1.16		{q14, q15}, [r1]!		@src 18 22 26 30

		tx_o16_0	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[0]
		vsub.s32	q9, q10, q11			@e_32[15]
		vpush		{q9}
		vadd.s32	q10,  q10, q11			@e_32[0]
		
		tx_o16_1	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[1]
		vsub.s32	q9, q6, q11				@e_32[14]
		vpush		{q9}
		vadd.s32	q6, q6, q11				@e_32[1]
		
		tx_o16_2	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[2]
		vsub.s32	q9, q7, q11				@e_32[13]
		vpush		{q9}
		vadd.s32	q7, q7, q11				@e_32[2]
		
		tx_o16_3	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[3]
		vsub.s32	q9, q8, q11				@e_32[12]
		vpush		{q9}
		vadd.s32	q8, q8, q11				@e_32[3]
		
		tx_o16_4	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[4]
		vsub.s32	q9, q5, q11				@e_32[11]
		vpush		{q9}
		vadd.s32	q5, q5, q11				@e_32[4]
		
		tx_o16_5	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[5]
		vsub.s32	q9, q4, q11				@e_32[10]
		vpush		{q9}
		vadd.s32	q4, q4, q11				@e_32[5]
		
		tx_o16_6	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[6]
		vsub.s32	q9, q3, q11				@e_32[9]
		vpush		{q9}
		vadd.s32	q3, q3, q11				@e_32[6]
		
		tx_o16_7	q11, d24, d25, d26, d27, d28, d29, d30, d31, d2, d3		@o_16[7]
		vsub.s32	q9, q2, q11				@e_32[8]
		vpush		{q9}
		vadd.s32	q2, q2, q11				@e_32[7]
#		vpush		{q2 - q5}
		vpush		{q2}
		vpush		{q3}
		vpush		{q4}
		vpush		{q5}
		vpush		{q6-q8}
		vpush		{q10}
.endm

.macro	tx31x1_h	dd0, dd1, idx, shift
		vpop		{q6}					@e_32[0]
		tx_o32_\idx	q7, d0, d1, d2, d3		@o_32[0]
		vadd.s32	q5, q6, q7
		vsub.s32	q6, q6, q7
		vqrshrn.s32	\dd0, q5, \shift		@delta0
		vqrshrn.s32	\dd1, q6, \shift		@delta31
.endm
.macro	tx32x4_h

		tx16x4_h_set 
		vld1.16		{q0, q1}, [r3]			@q0:90 90 88 85 82 78 73 67
											@q1:61 54 46 38 31 22 13  4
		sub		r3, r3, #32					@r3:Tx4_idx
		vld1.16		{q8, q9}, [r1]!			@src  1  3  5  7
		vld1.16		{q10, q11}, [r1]!		@src  9 11 13 15
		vld1.16		{q12, q13}, [r1]!		@src 17 19 21 23
		vld1.16		{q14, q15}, [r1]!		@src 15 27 29 31
		tx31x1_h	d4, d5, 0, #12
		tx31x1_h	d6, d9, 1, #12
		tx31x1_h	d8, d7, 2, #12
		tx31x1_h	d10, d11, 3, #12
		mov		lr, r0
		vld1.32		{d12[0]}, [lr], r2
		vld1.32		{d13[0]}, [lr], r2
		vld1.32		{d14[0]}, [lr], r2
		vld1.32		{d15[0]}, [lr], r2
		add		lr, r0, #28
		vld1.32		{d12[1]}, [lr], r2
		vld1.32		{d13[1]}, [lr], r2
		vld1.32		{d14[1]}, [lr], r2
		vld1.32		{d15[1]}, [lr], r2
		vswp		d5, d11
		vtrn.16		q2, q3
		vtrn.16		q4, q5
		vtrn.32		q2, q4
		vtrn.32		q3, q5
		vaddw.u8	q2, q2, d12
		vaddw.u8	q3, q3, d13
		vaddw.u8	q4, q4, d14
		vaddw.u8	q5, q5, d15
		vqmovun.s16	d4, q2
		vqmovun.s16	d5, q3
		vqmovun.s16	d6, q4
		vqmovun.s16	d7, q5
		mov		lr, r0
		vst1.32		{d4[0]}, [lr], r2
		vst1.32		{d5[0]}, [lr], r2
		vst1.32		{d6[0]}, [lr], r2
		vst1.32		{d7[0]}, [lr], r2
		add		lr, r0, #28
		vst1.32		{d4[1]}, [lr], r2
		vst1.32		{d5[1]}, [lr], r2
		vst1.32		{d6[1]}, [lr], r2
		vst1.32		{d7[1]}, [lr], r2
		
		tx31x1_h	d4, d5, 4, #12
		tx31x1_h	d6, d9, 5, #12
		tx31x1_h	d8, d7, 6, #12
		tx31x1_h	d10, d11, 7, #12
		add		lr, r0, #4
		vld1.32		{d12[0]}, [lr], r2
		vld1.32		{d13[0]}, [lr], r2
		vld1.32		{d14[0]}, [lr], r2
		vld1.32		{d15[0]}, [lr], r2
		add		lr, r0, #24
		vld1.32		{d12[1]}, [lr], r2
		vld1.32		{d13[1]}, [lr], r2
		vld1.32		{d14[1]}, [lr], r2
		vld1.32		{d15[1]}, [lr], r2
		vswp		d5, d11
		vtrn.16		q2, q3
		vtrn.16		q4, q5
		vtrn.32		q2, q4
		vtrn.32		q3, q5
		vaddw.u8	q2, q2, d12
		vaddw.u8	q3, q3, d13
		vaddw.u8	q4, q4, d14
		vaddw.u8	q5, q5, d15
		vqmovun.s16	d4, q2
		vqmovun.s16	d5, q3
		vqmovun.s16	d6, q4
		vqmovun.s16	d7, q5
		add		lr, r0, #4
		vst1.32		{d4[0]}, [lr], r2
		vst1.32		{d5[0]}, [lr], r2
		vst1.32		{d6[0]}, [lr], r2
		vst1.32		{d7[0]}, [lr], r2
		add		lr, r0, #24
		vst1.32		{d4[1]}, [lr], r2
		vst1.32		{d5[1]}, [lr], r2
		vst1.32		{d6[1]}, [lr], r2
		vst1.32		{d7[1]}, [lr], r2

		tx31x1_h	d4, d5, 8, #12
		tx31x1_h	d6, d9, 9, #12
		tx31x1_h	d8, d7, 10, #12
		tx31x1_h	d10, d11, 11, #12
		add		lr, r0, #8
		vld1.32		{d12[0]}, [lr], r2
		vld1.32		{d13[0]}, [lr], r2
		vld1.32		{d14[0]}, [lr], r2
		vld1.32		{d15[0]}, [lr], r2
		add		lr, r0, #20
		vld1.32		{d12[1]}, [lr], r2
		vld1.32		{d13[1]}, [lr], r2
		vld1.32		{d14[1]}, [lr], r2
		vld1.32		{d15[1]}, [lr], r2
		vswp		d5, d11
		vtrn.16		q2, q3
		vtrn.16		q4, q5
		vtrn.32		q2, q4
		vtrn.32		q3, q5
		vaddw.u8	q2, q2, d12
		vaddw.u8	q3, q3, d13
		vaddw.u8	q4, q4, d14
		vaddw.u8	q5, q5, d15
		vqmovun.s16	d4, q2
		vqmovun.s16	d5, q3
		vqmovun.s16	d6, q4
		vqmovun.s16	d7, q5
		add		lr, r0, #8
		vst1.32		{d4[0]}, [lr], r2
		vst1.32		{d5[0]}, [lr], r2
		vst1.32		{d6[0]}, [lr], r2
		vst1.32		{d7[0]}, [lr], r2
		add		lr, r0, #20
		vst1.32		{d4[1]}, [lr], r2
		vst1.32		{d5[1]}, [lr], r2
		vst1.32		{d6[1]}, [lr], r2
		vst1.32		{d7[1]}, [lr], r2

		tx31x1_h	d4, d5, 12, #12
		tx31x1_h	d6, d9, 13, #12
		tx31x1_h	d8, d7, 14, #12
		tx31x1_h	d10, d11, 15, #12
		add		lr, r0, #12
		vld1.8		{d12}, [lr], r2
		vld1.8		{d13}, [lr], r2
		vld1.8		{d14}, [lr], r2
		vld1.8		{d15}, [lr], r2
		vswp		d5, d11
		vtrn.16		q2, q3
		vtrn.16		q4, q5
		vtrn.32		q2, q4
		vtrn.32		q3, q5
		vaddw.u8	q2, q2, d12
		vaddw.u8	q3, q3, d13
		vaddw.u8	q4, q4, d14
		vaddw.u8	q5, q5, d15
		vqmovun.s16	d4, q2
		vqmovun.s16	d5, q3
		vqmovun.s16	d6, q4
		vqmovun.s16	d7, q5
		add		lr, r0, #12
		vst1.8		{d4}, [lr], r2
		vst1.8		{d5}, [lr], r2
		vst1.8		{d6}, [lr], r2
		vst1.8		{d7}, [lr], r2
.endm

Tx4_idx1:
.short 83, 36, 0, 0
Tx8_idx1:
.short 89, 75, 50, 18
Tx16_idx1:
.short 90, 87, 80, 70, 57, 43, 25, 9
Tx32_idx1:
.short 90, 90, 88, 85, 82, 78, 73, 67, 61, 54, 46, 38, 31, 22, 13, 4

#====================================================================
#void transform_32x32_add(
#r0:    uint8_t *_dst, 
#r1:	int16_t *coeffs, 
#r2:	ptrdiff_t _stride, 
#r3:	int col_limit);
#====================================================================
function ff_hevc_transform_32x32_add_neon, export=1
	stmfd	sp!, {r4 - r8, lr}
		mov		r12, #256
		adr		r3, Tx4_idx1
		lsr		r6, r12, #1					@r6: 128
		lsr		r4, r6, #1					@r4: 64
		rsb		r5, r4, #0					@r5:-64
		add		r8, r1, #1984				@r8->src31
		tx32x4_v
		add		r1, r1, #8
		tx32x4_v
		add		r1, r1, #8
		tx32x4_v
		add		r1, r1, #8
		tx32x4_v
		add		r1, r1, #8
		tx32x4_v
		add		r1, r1, #8
		tx32x4_v
		add		r1, r1, #8
		tx32x4_v
		add		r1, r1, #8
		tx32x4_v
		
#---------------------------------------------------------------------		
		sub		r1, r1, #56
		tx32x4_h
		add		r0, r0, r2, lsl #2
		tx32x4_h
		add		r0, r0, r2, lsl #2
		tx32x4_h
		add		r0, r0, r2, lsl #2
		tx32x4_h
		add		r0, r0, r2, lsl #2
		tx32x4_h
		add		r0, r0, r2, lsl #2
		tx32x4_h
		add		r0, r0, r2, lsl #2
		tx32x4_h
		add		r0, r0, r2, lsl #2
		tx32x4_h
	ldmfd	sp!, {r4 - r8, pc}
endfunc

