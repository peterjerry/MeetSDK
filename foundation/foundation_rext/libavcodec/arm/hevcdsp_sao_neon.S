#include "libavutil/arm/asm.S"
.text
.align 4	
Edge_idx:
	.byte	1, 2, 0, 3, 4, 0, 0, 0
#==============================================================================
#static void sao_edge_filter()
#r0:       uint8_t *_dst, 
#r1:       uint8_t *_src, 
#r2:     ptrdiff_t  stride,
#r3:     SAOParams *sao, 
#[sp, #0]      int  width, 
#[sp, #4]      int  height, 
#[sp, #8]      int  c_idx, 
#				init_x = init_y = 0;
#==============================================================================
function sao_edge_filter_neon, export=1
	pld    [r3]
        pld    [r1]
    stmfd   sp!, {r4 - r6, lr}
		adr 	lr, Edge_idx
		ldr		r4, [sp, #16]				@r4:width
		ldr		r5, [sp, #20]				@r5:height
		ldr		r6, [sp, #24]				@r6:c_idx
		vld1.8		{d31}, [lr]				@d31:edge_idx:1,2,0,3,4,0,0,0
		add		r3, r3, #108
		add		r3, r3, r6, lsl #2			@sao += 108+4*c_idx
		ldr		lr, [r3]					@lr:sao_eo_class
		add		r3, r3, #12
		add		r3, r3, r6					@sao += 12+c_idx
		vld1.8		{d30}, [r3]				@d30:sao_offset_val

		vmov.i8		q14, #2					@d29:2 2 2 2 2 2 2 2
		cmp		r4, #8
		ble		sao_edge_filter_w8_neon
		cmp		lr, #1
		beq		sao_edge_V
		cmp		lr, #2
		beq		sao_edge_45D
		cmp		lr, #3
		beq		sao_edge_135D

#-------------------------=-----------------------------------------		
sao_edge_H:		
		subs	r1, r1, #1					@src -= 1
1:
		pld		[r1]
		mov		lr, r0
		mov		r3, r1
		mov		r6, r4						@r6:width
2:
		vld1.8		{q0}, [r3]!				@q0: p[-1~14]
		vld1.8		{d2}, [r3]				@d2: p[15~22]
		vext.8		q2, q0, q1, #1			@q2: p[ 0~15]
		vext.8		q3, q0, q1, #2			@q3: p[ 1~16]
		vcgt.u8		q4, q0, q2
		vclt.u8		q5, q0, q2
		vsub.s8		q4, q4, q5
		vcgt.u8		q5, q3, q2
		vclt.u8		q6, q3, q2
		vsub.s8		q5, q5, q6
		vadd.s8		q4, q4, q5
		vadd.s8		q4, q4, q14
		vtbl.8		d10, {d31}, d8			@q5:offset_val
		vtbl.8		d11, {d31}, d9			@q5:offset_val
		vtbl.8		d8, {d30}, d10			@q4:sao_offset_val[offset_val]
		vtbl.8		d9, {d30}, d11			@q4:sao_offset_val[offset_val]
		vmovl.u8	q0, d4
		vmovl.u8	q1, d5
		vaddw.s8	q0, q0, d8
		vaddw.s8	q1, q1, d9
		vqmovun.s16	d0, q0
		vqmovun.s16	d1, q1
		vst1.8		{q0}, [lr]!
		subs	r6, r6, #16
		bgt		2b
		add		r0, r0, r2
		add		r1, r1, r2
		subs	r5, r5, #1
		bgt		1b
	ldmfd	sp!, {r4 - r6, pc}
		
#---------------------||-------------------------------------------------		
sao_edge_V:		
		subs	r1, r1, r2					@src -= stride
1:
		pld		[r1]
		pld		[r1, r2]
		pld		[r1, r2, lsl #1]
		mov		lr, r0
		mov		r3, r1
		mov		r6, r5						@r6:height
		vld1.8		{q0}, [r3], r2				@q0: p[-1][0~15]
		vld1.8		{q1}, [r3], r2				@q1: p[ 0][0~15]
2:
		vld1.8		{q2}, [r3], r2				@q2: p[ 1][0~15]
		pld		[r3]
		vcgt.u8		q4, q0, q1
		vclt.u8		q5, q0, q1
		vsub.s8		q4, q4, q5
		vcgt.u8		q5, q2, q1
		vclt.u8		q6, q2, q1
		vsub.s8		q5, q5, q6
		vadd.s8		q4, q4, q5
		vadd.s8		q4, q4, q14
		vtbl.8		d10, {d31}, d8			@q5:offset_val
		vtbl.8		d11, {d31}, d9			@q5:offset_val
		vtbl.8		d8, {d30}, d10			@q4:sao_offset_val[offset_val]
		vtbl.8		d9, {d30}, d11			@q4:sao_offset_val[offset_val]
		vmovl.u8	q6, d2
		vmovl.u8	q5, d3
		vaddw.s8	q6, q6, d8
		vaddw.s8	q5, q5, d9
		vqmovun.s16	d8, q6
		vqmovun.s16	d9, q5
		vst1.8		{q4}, [lr], r2
		
		vld1.8		{q3}, [r3], r2				@q2: p[ 1][0~15]
		pld		[r3]
		vcgt.u8		q4, q1, q2
		vclt.u8		q5, q1, q2
		vsub.s8		q4, q4, q5
		vcgt.u8		q5, q3, q2
		vclt.u8		q6, q3, q2
		vsub.s8		q5, q5, q6
		vadd.s8		q4, q4, q5
		vadd.s8		q4, q4, q14
		vtbl.8		d10, {d31}, d8			@q5:offset_val
		vtbl.8		d11, {d31}, d9			@q5:offset_val
		vtbl.8		d8, {d30}, d10			@q4:sao_offset_val[offset_val]
		vtbl.8		d9, {d30}, d11			@q4:sao_offset_val[offset_val]
		vmovl.u8	q6, d4
		vmovl.u8	q5, d5
		vaddw.s8	q6, q6, d8
		vaddw.s8	q5, q5, d9
		vqmovun.s16	d8, q6
		vqmovun.s16	d9, q5
		vst1.8		{q4}, [lr], r2
		
		vld1.8		{q0}, [r3], r2				@q2: p[ 1][0~15]
		pld		[r3]
		vcgt.u8		q4, q2, q3
		vclt.u8		q5, q2, q3
		vsub.s8		q4, q4, q5
		vcgt.u8		q5, q0, q3
		vclt.u8		q6, q0, q3
		vsub.s8		q5, q5, q6
		vadd.s8		q4, q4, q5
		vadd.s8		q4, q4, q14
		vtbl.8		d10, {d31}, d8			@q5:offset_val
		vtbl.8		d11, {d31}, d9			@q5:offset_val
		vtbl.8		d8, {d30}, d10			@q4:sao_offset_val[offset_val]
		vtbl.8		d9, {d30}, d11			@q4:sao_offset_val[offset_val]
		vmovl.u8	q6, d6
		vmovl.u8	q5, d7
		vaddw.s8	q6, q6, d8
		vaddw.s8	q5, q5, d9
		vqmovun.s16	d8, q6
		vqmovun.s16	d9, q5
		vst1.8		{q4}, [lr], r2
		
		vld1.8		{q1}, [r3], r2				@q2: p[ 1][0~15]
		pld		[r3]
		vcgt.u8		q4, q3, q0
		vclt.u8		q5, q3, q0
		vsub.s8		q4, q4, q5
		vcgt.u8		q5, q1, q0
		vclt.u8		q6, q1, q0
		vsub.s8		q5, q5, q6
		vadd.s8		q4, q4, q5
		vadd.s8		q4, q4, q14
		vtbl.8		d10, {d31}, d8			@q5:offset_val
		vtbl.8		d11, {d31}, d9			@q5:offset_val
		vtbl.8		d8, {d30}, d10			@q4:sao_offset_val[offset_val]
		vtbl.8		d9, {d30}, d11			@q4:sao_offset_val[offset_val]
		vmovl.u8	q6, d0
		vmovl.u8	q5, d1
		vaddw.s8	q6, q6, d8
		vaddw.s8	q5, q5, d9
		vqmovun.s16	d8, q6
		vqmovun.s16	d9, q5
		vst1.8		{q4}, [lr], r2
		
		subs	r6, r6, #4
		bgt		2b
		add		r0, r0, #16
		add		r1, r1, #16
		subs	r4, r4, #16
		bgt		1b
	ldmfd	sp!, {r4 - r6, pc}
		
#---------------------\\-------------------------------------------------		
sao_edge_45D:		
		subs	r1, r1, r2
		subs	r1, r1, #1					@src -= stride+1
1:
		pld		[r1]
		pld		[r1, r2]
		pld		[r1, r2, lsl #1]
		mov		lr, r0
		mov		r3, r1
		mov		r6, r5						@r6:height
		vld1.8		{q0}, [r3], r2				@q0: p[-1][-1~14]
		vld1.8		{q2, q3}, [r3], r2			@q2: p[ 0][-1~14] q3: p[ 0][15~30]
2:
		vld1.8		{q4, q5}, [r3], r2			@q4: p[ 1][-1~14] q5: p[ 1][15~30]
		pld		[r3]
		vext.8		q8, q2, q3, #1				@q8: p[ 0][0~15]
		vext.8		q9, q4, q5, #2				@q9: p[ 1][1~16]
		vcgt.u8		q10, q0, q8
		vclt.u8		q11, q0, q8
		vsub.s8		q10, q10, q11
		vcgt.u8		q11, q9, q8
		vclt.u8		q12, q9, q8
		vsub.s8		q11, q11, q12
		vadd.s8		q10, q10, q11
		vadd.s8		q10, q10, q14
		vtbl.8		d22, {d31}, d20			@q11:offset_val
		vtbl.8		d23, {d31}, d21			@q11:offset_val
		vtbl.8		d20, {d30}, d22			@q10:sao_offset_val[offset_val]
		vtbl.8		d21, {d30}, d23			@q10:sao_offset_val[offset_val]
		vmovl.u8	q12, d16
		vmovl.u8	q11, d17
		vaddw.s8	q12, q12, d20
		vaddw.s8	q11, q11, d21
		vqmovun.s16	d20, q12
		vqmovun.s16	d21, q11
		vst1.8		{q10}, [lr], r2
		
		vld1.8		{q6, q7}, [r3], r2			@q6: p[ 1][-1~14] q7: p[ 1][15~30]
		pld		[r3]
		vext.8		q8, q4, q5, #1				@q8: p[ 0][0~15]
		vext.8		q9, q6, q7, #2				@q9: p[ 1][1~16]
		vcgt.u8		q10, q2, q8
		vclt.u8		q11, q2, q8
		vsub.s8		q10, q10, q11
		vcgt.u8		q11, q9, q8
		vclt.u8		q12, q9, q8
		vsub.s8		q11, q11, q12
		vadd.s8		q10, q10, q11
		vadd.s8		q10, q10, q14
		vtbl.8		d22, {d31}, d20			@q11:offset_val
		vtbl.8		d23, {d31}, d21			@q11:offset_val
		vtbl.8		d20, {d30}, d22			@q10:sao_offset_val[offset_val]
		vtbl.8		d21, {d30}, d23			@q10:sao_offset_val[offset_val]
		vmovl.u8	q12, d16
		vmovl.u8	q11, d17
		vaddw.s8	q12, q12, d20
		vaddw.s8	q11, q11, d21
		vqmovun.s16	d20, q12
		vqmovun.s16	d21, q11
		vst1.8		{q10}, [lr], r2
		
		vld1.8		{q0, q1}, [r3], r2			@q6: p[ 1][-1~14] q7: p[ 1][15~30]
		pld		[r3]
		vext.8		q8, q6, q7, #1				@q8: p[ 0][0~15]
		vext.8		q9, q0, q1, #2				@q9: p[ 1][1~16]
		vcgt.u8		q10, q4, q8
		vclt.u8		q11, q4, q8
		vsub.s8		q10, q10, q11
		vcgt.u8		q11, q9, q8
		vclt.u8		q12, q9, q8
		vsub.s8		q11, q11, q12
		vadd.s8		q10, q10, q11
		vadd.s8		q10, q10, q14
		vtbl.8		d22, {d31}, d20			@q11:offset_val
		vtbl.8		d23, {d31}, d21			@q11:offset_val
		vtbl.8		d20, {d30}, d22			@q10:sao_offset_val[offset_val]
		vtbl.8		d21, {d30}, d23			@q10:sao_offset_val[offset_val]
		vmovl.u8	q12, d16
		vmovl.u8	q11, d17
		vaddw.s8	q12, q12, d20
		vaddw.s8	q11, q11, d21
		vqmovun.s16	d20, q12
		vqmovun.s16	d21, q11
		vst1.8		{q10}, [lr], r2
		
		vld1.8		{q2, q3}, [r3], r2			@q6: p[ 1][-1~14] q7: p[ 1][15~30]
		pld		[r3]
		vext.8		q8, q0, q1, #1				@q8: p[ 0][0~15]
		vext.8		q9, q2, q3, #2				@q9: p[ 1][1~16]
		vcgt.u8		q10, q6, q8
		vclt.u8		q11, q6, q8
		vsub.s8		q10, q10, q11
		vcgt.u8		q11, q9, q8
		vclt.u8		q12, q9, q8
		vsub.s8		q11, q11, q12
		vadd.s8		q10, q10, q11
		vadd.s8		q10, q10, q14
		vtbl.8		d22, {d31}, d20			@q11:offset_val
		vtbl.8		d23, {d31}, d21			@q11:offset_val
		vtbl.8		d20, {d30}, d22			@q10:sao_offset_val[offset_val]
		vtbl.8		d21, {d30}, d23			@q10:sao_offset_val[offset_val]
		vmovl.u8	q12, d16
		vmovl.u8	q11, d17
		vaddw.s8	q12, q12, d20
		vaddw.s8	q11, q11, d21
		vqmovun.s16	d20, q12
		vqmovun.s16	d21, q11
		vst1.8		{q10}, [lr], r2
		
		subs	r6, r6, #4
		bgt		2b
		add		r0, r0, #16
		add		r1, r1, #16
		subs	r4, r4, #16
		bgt		1b
	ldmfd	sp!, {r4 - r6, pc}
		
#---------------------//-------------------------------------------------		
sao_edge_135D:		
		subs	r1, r1, r2
		subs	r1, r1, #1					@src -= stride+1
1:
		pld		[r1]
		pld		[r1, r2]
		pld		[r1, r2, lsl #1]
		mov		lr, r0
		mov		r3, r1
		mov		r6, r5						@r6:height
		vld1.8		{q0, q1}, [r3], r2				@q0: p[-1][-1~14]
		vld1.8		{q2, q3}, [r3], r2			@q2: p[ 0][-1~14] q3: p[ 0][15~30]
2:
		vld1.8		{q4, q5}, [r3], r2			@q4: p[ 1][-1~14] q5: p[ 1][15~30]
		pld		[r3]
		vext.8		q8, q0, q1, #2				@q8: p[-1][1~16]
		vext.8		q9, q2, q3, #1				@q9: p[ 0][0~15]
		vcgt.u8		q10, q8, q9
		vclt.u8		q11, q8, q9
		vsub.s8		q10, q10, q11
		vcgt.u8		q11, q4, q9
		vclt.u8		q12, q4, q9
		vsub.s8		q11, q11, q12
		vadd.s8		q10, q10, q11
		vadd.s8		q10, q10, q14
		vtbl.8		d22, {d31}, d20			@q11:offset_val
		vtbl.8		d23, {d31}, d21			@q11:offset_val
		vtbl.8		d20, {d30}, d22			@q10:sao_offset_val[offset_val]
		vtbl.8		d21, {d30}, d23			@q10:sao_offset_val[offset_val]
		vmovl.u8	q12, d18
		vmovl.u8	q11, d19
		vaddw.s8	q12, q12, d20
		vaddw.s8	q11, q11, d21
		vqmovun.s16	d20, q12
		vqmovun.s16	d21, q11
		vst1.8		{q10}, [lr], r2
		
		vld1.8		{q6, q7}, [r3], r2			@q6: p[ 1][-1~14] q7: p[ 1][15~30]
		pld		[r3]
		vext.8		q8, q2, q3, #2				@q8: p[-1][1~16]
		vext.8		q9, q4, q5, #1				@q9: p[ 0][0~15]
		vcgt.u8		q10, q8, q9
		vclt.u8		q11, q8, q9
		vsub.s8		q10, q10, q11
		vcgt.u8		q11, q6, q9
		vclt.u8		q12, q6, q9
		vsub.s8		q11, q11, q12
		vadd.s8		q10, q10, q11
		vadd.s8		q10, q10, q14
		vtbl.8		d22, {d31}, d20			@q11:offset_val
		vtbl.8		d23, {d31}, d21			@q11:offset_val
		vtbl.8		d20, {d30}, d22			@q10:sao_offset_val[offset_val]
		vtbl.8		d21, {d30}, d23			@q10:sao_offset_val[offset_val]
		vmovl.u8	q12, d18
		vmovl.u8	q11, d19
		vaddw.s8	q12, q12, d20
		vaddw.s8	q11, q11, d21
		vqmovun.s16	d20, q12
		vqmovun.s16	d21, q11
		vst1.8		{q10}, [lr], r2
		
		vld1.8		{q0, q1}, [r3], r2			@q0: p[ 1][-1~14] q1: p[ 1][15~30]
		pld		[r3]
		vext.8		q8, q4, q5, #2				@q8: p[-1][1~16]
		vext.8		q9, q6, q7, #1				@q9: p[ 0][0~15]
		vcgt.u8		q10, q8, q9
		vclt.u8		q11, q8, q9
		vsub.s8		q10, q10, q11
		vcgt.u8		q11, q0, q9
		vclt.u8		q12, q0, q9
		vsub.s8		q11, q11, q12
		vadd.s8		q10, q10, q11
		vadd.s8		q10, q10, q14
		vtbl.8		d22, {d31}, d20			@q11:offset_val
		vtbl.8		d23, {d31}, d21			@q11:offset_val
		vtbl.8		d20, {d30}, d22			@q10:sao_offset_val[offset_val]
		vtbl.8		d21, {d30}, d23			@q10:sao_offset_val[offset_val]
		vmovl.u8	q12, d18
		vmovl.u8	q11, d19
		vaddw.s8	q12, q12, d20
		vaddw.s8	q11, q11, d21
		vqmovun.s16	d20, q12
		vqmovun.s16	d21, q11
		vst1.8		{q10}, [lr], r2
		
		vld1.8		{q2, q3}, [r3], r2			@q2: p[ 1][-1~14] q3: p[ 1][15~30]
		pld		[r3]
		vext.8		q8, q6, q7, #2				@q8: p[-1][1~16]
		vext.8		q9, q0, q1, #1				@q9: p[ 0][0~15]
		vcgt.u8		q10, q8, q9
		vclt.u8		q11, q8, q9
		vsub.s8		q10, q10, q11
		vcgt.u8		q11, q2, q9
		vclt.u8		q12, q2, q9
		vsub.s8		q11, q11, q12
		vadd.s8		q10, q10, q11
		vadd.s8		q10, q10, q14
		vtbl.8		d22, {d31}, d20			@q11:offset_val
		vtbl.8		d23, {d31}, d21			@q11:offset_val
		vtbl.8		d20, {d30}, d22			@q10:sao_offset_val[offset_val]
		vtbl.8		d21, {d30}, d23			@q10:sao_offset_val[offset_val]
		vmovl.u8	q12, d18
		vmovl.u8	q11, d19
		vaddw.s8	q12, q12, d20
		vaddw.s8	q11, q11, d21
		vqmovun.s16	d20, q12
		vqmovun.s16	d21, q11
		vst1.8		{q10}, [lr], r2
		
		subs	r6, r6, #4
		bgt		2b
		add		r0, r0, #16
		add		r1, r1, #16
		subs	r4, r4, #16
		bgt		1b
	ldmfd	sp!, {r4 - r6, pc}

sao_edge_filter_w8_neon:
		cmp		lr, #1
		beq		sao_edge_V1
		cmp		lr, #2
		beq		sao_edge_45D1
		cmp		lr, #3
		beq		sao_edge_135D1
		
#---------------------=---------------------------------------------		
sao_edge_H1:		
		subs	r1, r1, #1					@src -= 1
1:
		mov		lr, r0
		mov		r3, r1
		mov		r6, r4						@r6:width
2:
		vld1.8		{q0}, [r3]				@d0: p[-1~6]
		add		r3, r3, #8
		vext.8		d2, d0, d1, #1			@d2: p[ 0~7]
		vext.8		d3, d0, d1, #2			@d3: p[ 1~8]
		vcgt.u8		d4, d0, d2
		vclt.u8		d5, d0, d2
		vsub.s8		d4, d4, d5
		vcgt.u8		d5, d3, d2
		vclt.u8		d6, d3, d2
		vsub.s8		d5, d5, d6
		vadd.s8		d4, d4, d5
		vadd.s8		d4, d4, d29
		vtbl.8		d5, {d31}, d4			@d5:offset_val
		vtbl.8		d4, {d30}, d5			@d4:sao_offset_val[offset_val]
		vmovl.u8	q1, d2
		vaddw.s8	q1, q1, d4
		vqmovun.s16	d2, q1
		vst1.8		{d2}, [lr]!
		subs	r6, r6, #8
		bgt		2b
		add		r0, r0, r2
		add		r1, r1, r2
		subs	r5, r5, #1
		bgt		1b
	ldmfd	sp!, {r4 - r6, pc}
	
#----------------------||--------------------------------------------			
sao_edge_V1:		
		subs	r1, r1, r2					@src -= stride
1:
		mov		lr, r0
		mov		r3, r1
		mov		r6, r5						@r6:height
		vld1.8		{d0}, [r3], r2				@d0: p[-1][0~7]
		vld1.8		{d1}, [r3], r2				@d1: p[ 0][0~7]
2:
		vld1.8		{d2}, [r3], r2				@d2: p[ 1][0~7]
		vcgt.u8		d4, d0, d1
		vclt.u8		d5, d0, d1
		vsub.s8		d4, d4, d5
		vcgt.u8		d5, d2, d1
		vclt.u8		d6, d2, d1
		vsub.s8		d5, d5, d6
		vadd.s8		d4, d4, d5
		vadd.s8		d4, d4, d29
		vtbl.8		d5, {d31}, d4			@d5:offset_val
		vtbl.8		d4, {d30}, d5			@d4:sao_offset_val[offset_val]
		vmovl.u8	q3, d1
		vaddw.s8	q3, q3, d4
		vqmovun.s16	d5, q3
		vst1.8		{d5}, [lr], r2
		
		vld1.8		{d3}, [r3], r2
		vcgt.u8		d4, d1, d2
		vclt.u8		d5, d1, d2
		vsub.s8		d4, d4, d5
		vcgt.u8		d5, d3, d2
		vclt.u8		d6, d3, d2
		vsub.s8		d5, d5, d6
		vadd.s8		d4, d4, d5
		vadd.s8		d4, d4, d29
		vtbl.8		d5, {d31}, d4			@d5:offset_val
		vtbl.8		d4, {d30}, d5			@d4:sao_offset_val[offset_val]
		vmovl.u8	q3, d2
		vaddw.s8	q3, q3, d4
		vqmovun.s16	d4, q3
		vst1.8		{d4}, [lr], r2
		
		vld1.8		{d0}, [r3], r2
		vcgt.u8		d4, d2, d3
		vclt.u8		d5, d2, d3
		vsub.s8		d4, d4, d5
		vcgt.u8		d5, d0, d3
		vclt.u8		d6, d0, d3
		vsub.s8		d5, d5, d6
		vadd.s8		d4, d4, d5
		vadd.s8		d4, d4, d29
		vtbl.8		d5, {d31}, d4			@d5:offset_val
		vtbl.8		d4, {d30}, d5			@d4:sao_offset_val[offset_val]
		vmovl.u8	q3, d3
		vaddw.s8	q3, q3, d4
		vqmovun.s16	d4, q3
		vst1.8		{d4}, [lr], r2
		
		vld1.8		{d1}, [r3], r2
		vcgt.u8		d4, d3, d0
		vclt.u8		d5, d3, d0
		vsub.s8		d4, d4, d5
		vcgt.u8		d5, d1, d0
		vclt.u8		d6, d1, d0
		vsub.s8		d5, d5, d6
		vadd.s8		d4, d4, d5
		vadd.s8		d4, d4, d29
		vtbl.8		d5, {d31}, d4			@d5:offset_val
		vtbl.8		d4, {d30}, d5			@d4:sao_offset_val[offset_val]
		vmovl.u8	q3, d0
		vaddw.s8	q3, q3, d4
		vqmovun.s16	d4, q3
		vst1.8		{d4}, [lr], r2
		
		subs	r6, r6, #4
		bgt		2b
		add		r0, r0, #8
		add		r1, r1, #8
		subs	r4, r4, #8
		bgt		1b
	ldmfd	sp!, {r4 - r6, pc}
		
#---------------------\\---------------------------------------------		
sao_edge_45D1:			
		subs	r1, r1, r2
		subs	r1, r1, #1					@src -= stride+1
1:
		mov		lr, r0
		mov		r3, r1
		mov		r6, r5						@r6:height
		vld1.8		{q0}, [r3], r2				@q0: p[-1][-1~14]
		vld1.8		{q1}, [r3], r2				@q1: p[ 0][-1~14]
2:
		vld1.8		{q2}, [r3], r2				@q2: p[ 1][-1~14]
		vext.8		d8, d2, d3, #1				@d8: p[0][0~7]
		vcgt.u8		d10, d0, d8
		vclt.u8		d11, d0, d8
		vsub.s8		d10, d10, d11
		vext.8		d9, d4, d5, #2				@d9: p[1][1~8]
		vcgt.u8		d11, d9, d8
		vclt.u8		d12, d9, d8
		vsub.s8		d11, d11, d12
		vadd.s8		d10, d10, d11
		vadd.s8		d10, d10, d29
		vtbl.8		d11, {d31}, d10			@d11:offset_val
		vtbl.8		d10, {d30}, d11			@d10:sao_offset_val[offset_val]
		vmovl.u8	q6, d8
		vaddw.s8	q6, q6, d10
		vqmovun.s16	d10, q6
		vst1.8		{d10}, [lr], r2
		
		vld1.8		{q3}, [r3], r2				@q3: p[ 1][-1~14]
		vext.8		d8, d4, d5, #1				@d8: p[0][0~7]
		vcgt.u8		d10, d2, d8
		vclt.u8		d11, d2, d8
		vsub.s8		d10, d10, d11
		vext.8		d9, d6, d7, #2				@d9: p[1][1~8]
		vcgt.u8		d11, d9, d8
		vclt.u8		d12, d9, d8
		vsub.s8		d11, d11, d12
		vadd.s8		d10, d10, d11
		vadd.s8		d10, d10, d29
		vtbl.8		d11, {d31}, d10			@d11:offset_val
		vtbl.8		d10, {d30}, d11			@d10:sao_offset_val[offset_val]
		vmovl.u8	q6, d8
		vaddw.s8	q6, q6, d10
		vqmovun.s16	d10, q6
		vst1.8		{d10}, [lr], r2
		
		vld1.8		{q0}, [r3], r2				@q0: p[ 1][-1~14]
		vext.8		d8, d6, d7, #1				@d8: p[0][0~7]
		vcgt.u8		d10, d4, d8
		vclt.u8		d11, d4, d8
		vsub.s8		d10, d10, d11
		vext.8		d9, d0, d1, #2				@d9: p[1][1~8]
		vcgt.u8		d11, d9, d8
		vclt.u8		d12, d9, d8
		vsub.s8		d11, d11, d12
		vadd.s8		d10, d10, d11
		vadd.s8		d10, d10, d29
		vtbl.8		d11, {d31}, d10			@d11:offset_val
		vtbl.8		d10, {d30}, d11			@d10:sao_offset_val[offset_val]
		vmovl.u8	q6, d8
		vaddw.s8	q6, q6, d10
		vqmovun.s16	d10, q6
		vst1.8		{d10}, [lr], r2
		
		vld1.8		{q1}, [r3], r2				@q1: p[ 1][-1~14]
		vext.8		d8, d0, d1, #1				@d8: p[0][0~7]
		vcgt.u8		d10, d6, d8
		vclt.u8		d11, d6, d8
		vsub.s8		d10, d10, d11
		vext.8		d9, d2, d3, #2				@d9: p[1][1~8]
		vcgt.u8		d11, d9, d8
		vclt.u8		d12, d9, d8
		vsub.s8		d11, d11, d12
		vadd.s8		d10, d10, d11
		vadd.s8		d10, d10, d29
		vtbl.8		d11, {d31}, d10			@d11:offset_val
		vtbl.8		d10, {d30}, d11			@d10:sao_offset_val[offset_val]
		vmovl.u8	q6, d8
		vaddw.s8	q6, q6, d10
		vqmovun.s16	d10, q6
		vst1.8		{d10}, [lr], r2
		
		subs	r6, r6, #4
		bgt		2b
		add		r0, r0, #8
		add		r1, r1, #8
		subs	r4, r4, #8
		bgt		1b
	ldmfd	sp!, {r4 - r6, pc}
		
#---------------------//---------------------------------------------	
sao_edge_135D1:	
		subs	r1, r1, r2
		subs	r1, r1, #1					@src -= stride+1
1:
		mov		lr, r0
		mov		r3, r1
		mov		r6, r5						@r6:height
		vld1.8		{q0}, [r3], r2				@q0: p[-1][-1~14]
		vld1.8		{q1}, [r3], r2				@q1: p[ 0][-1~14]
2:
		vld1.8		{q2}, [r3], r2				@q2: p[ 1][-1~14]
		vext.8		d8, d0, d1, #2				@d8: p[-1][1~8]
		vext.8		d9, d2, d3, #1				@d9: p[0][0~7]
		vcgt.u8		d10, d8, d9
		vclt.u8		d11, d8, d9
		vsub.s8		d10, d10, d11
		vcgt.u8		d11, d4, d9
		vclt.u8		d12, d4, d9
		vsub.s8		d11, d11, d12
		vadd.s8		d10, d10, d11
		vadd.s8		d10, d10, d29
		vtbl.8		d11, {d31}, d10			@d11:offset_val
		vtbl.8		d10, {d30}, d11			@d10:sao_offset_val[offset_val]
		vmovl.u8	q6, d9
		vaddw.s8	q6, q6, d10
		vqmovun.s16	d10, q6
		vst1.8		{d10}, [lr], r2
		
		vld1.8		{q3}, [r3], r2				@q2: p[ 1][-1~14]
		vext.8		d8, d2, d3, #2				@d8: p[-1][1~8]
		vext.8		d9, d4, d5, #1				@d9: p[0][0~7]
		vcgt.u8		d10, d8, d9
		vclt.u8		d11, d8, d9
		vsub.s8		d10, d10, d11
		vcgt.u8		d11, d6, d9
		vclt.u8		d12, d6, d9
		vsub.s8		d11, d11, d12
		vadd.s8		d10, d10, d11
		vadd.s8		d10, d10, d29
		vtbl.8		d11, {d31}, d10			@d11:offset_val
		vtbl.8		d10, {d30}, d11			@d10:sao_offset_val[offset_val]
		vmovl.u8	q6, d9
		vaddw.s8	q6, q6, d10
		vqmovun.s16	d10, q6
		vst1.8		{d10}, [lr], r2
		
		vld1.8		{q0}, [r3], r2				@q2: p[ 1][-1~14]
		vext.8		d8, d4, d5, #2				@d8: p[-1][1~8]
		vext.8		d9, d6, d7, #1				@d9: p[0][0~7]
		vcgt.u8		d10, d8, d9
		vclt.u8		d11, d8, d9
		vsub.s8		d10, d10, d11
		vcgt.u8		d11, d0, d9
		vclt.u8		d12, d0, d9
		vsub.s8		d11, d11, d12
		vadd.s8		d10, d10, d11
		vadd.s8		d10, d10, d29
		vtbl.8		d11, {d31}, d10			@d11:offset_val
		vtbl.8		d10, {d30}, d11			@d10:sao_offset_val[offset_val]
		vmovl.u8	q6, d9
		vaddw.s8	q6, q6, d10
		vqmovun.s16	d10, q6
		vst1.8		{d10}, [lr], r2
		
		vld1.8		{q1}, [r3], r2				@q2: p[ 1][-1~14]
		vext.8		d8, d6, d7, #2				@d8: p[-1][1~8]
		vext.8		d9, d0, d1, #1				@d9: p[0][0~7]
		vcgt.u8		d10, d8, d9
		vclt.u8		d11, d8, d9
		vsub.s8		d10, d10, d11
		vcgt.u8		d11, d2, d9
		vclt.u8		d12, d2, d9
		vsub.s8		d11, d11, d12
		vadd.s8		d10, d10, d11
		vadd.s8		d10, d10, d29
		vtbl.8		d11, {d31}, d10			@d11:offset_val
		vtbl.8		d10, {d30}, d11			@d10:sao_offset_val[offset_val]
		vmovl.u8	q6, d9
		vaddw.s8	q6, q6, d10
		vqmovun.s16	d10, q6
		vst1.8		{d10}, [lr], r2
		
		subs	r6, r6, #4
		bgt		2b
		add		r0, r0, #8
		add		r1, r1, #8
		subs	r4, r4, #8
		bgt		1b
	ldmfd	sp!, {r4 - r6, pc}
endfunc		
		
	
#==============================================================================
#static void sao_band_filter_0()
#r0:       uint8_t *dst, 
#r1:       uint8_t *_src, 
#r2:     ptrdiff_t  stride,
#r3:     SAOParams *sao,
#[sp, #0]      int *borders, 
#[sp, #4]      int  width, 
#[sp, #8]      int  height, 
#[sp, #12]     int  c_idx, 
#==============================================================================
function hevc_sao_band_filter_neon, export=1
	pld    [r3]
        pld    [r1]
    stmfd   sp!, {r4 - r6, lr}
		adr		lr, Table_idx
		pld		[lr]
		ldr		r4, [sp, #20]				@r4:width
		ldr		r5, [sp, #24]				@r5:height
		ldr		r6, [sp, #28]				@r6:c_idx
		vld1.8		{q0, q1}, [lr]			@Table_idx
		add		r3, r3, #96
		add		r3, r3, r6, lsl #2			@sao += 96+4*c_idx
		ldr		lr, [r3]					@lr:sao_left_class
		add		r3, r3, #25
		add		r3, r3, r6					@sao += 25+c_idx
		veor		d30, d30
		vld1.32		{d30[0]}, [r3]				@d30:sao_offset_val
		
		vdup.8		q2, lr
		vsub.u8		q0, q0, q2
		vsub.u8		q1, q1, q2
		vtbl.8		d0, {d30}, d0
		vtbl.8		d1, {d30}, d1
		vtbl.8		d2, {d30}, d2
		vtbl.8		d3, {d30}, d3
		
		cmp		r4, #8
		ble		hevc_sao_band_filter_w8
		
1:	
		mov		r3, r1
		mov		lr, r0
		mov		r6, r4
2:	
		vld1.8		{q2}, [r3]!
		vshr.u8		q3, q2, #3
		vtbl.8		d8, {d0, d1, d2, d3}, d6
		vtbl.8		d9, {d0, d1, d2, d3}, d7
		vmovl.u8	q5, d4
		vaddw.s8	q7, q5, d8
		vmovl.u8	q5, d5
		vaddw.s8	q6, q5, d9
		vqmovun.s16	d13, q6
		vqmovun.s16	d12, q7
		vst1.8		{q6}, [lr]!
		subs		r6, #16
		bgt		2b
		pld			[r1, r2]
		subs		r5, #1
		add			r0, r0, r2
		add			r1, r1, r2
		bgt		1b
		
		ldmfd	sp!, {r4 - r6, pc}
		
hevc_sao_band_filter_w8:
1:	
		mov		r3, r1
		mov		lr, r0
		mov		r6, r4
2:	
		vld1.8		{d4}, [r3]!
		vshr.u8		d5, d4, #3
		vtbl.8		d6, {d0, d1, d2, d3}, d5
		vmovl.u8	q5, d4
		vaddw.s8	q6, q5, d6
		vqmovun.s16	d12, q6
		vst1.8		{d12}, [lr]!
		subs		r6, #8
		bgt		2b
		pld			[r1, r2]
		subs		r5, #1
		add			r0, r0, r2
		add			r1, r1, r2
		bgt		1b
		
		ldmfd	sp!, {r4 - r6, pc}
endfunc		

	
#==============================================================================
#static void sao_band_filter_0()
#r0:       uint8_t *dst, 
#r1:       uint8_t *_src, 
#r2:     ptrdiff_t  stride,
#r3:          char *offset_table, 
#[sp, #0]      int  width, 
#[sp, #4]      int  height, 
#==============================================================================
function hevc_sao_band_filter_0_neon, export=1
	pld    [r3]
        pld    [r1]
        stmfd   sp!, {r4 - r6, lr}
		ldr		r4, [sp, #0x10]				@r4:width
		ldr		r5, [sp, #0x14]				@r5:height
		vld1.8		{q0, q1}, [r3]
		cmp		r4, #8
		ble		hevc_sao_band_filter_0_w8
		
1:	
		mov		r3, r1
		mov		lr, r0
		mov		r6, r4
2:	
		vld1.8		{q2}, [r3]!
		vshr.u8		q3, q2, #3
		vtbl.8		d8, {d0, d1, d2, d3}, d6
		vtbl.8		d9, {d0, d1, d2, d3}, d7
		vmovl.u8	q5, d4
		vaddw.s8	q7, q5, d8
		vmovl.u8	q5, d5
		vaddw.s8	q6, q5, d9
		vqmovun.s16	d13, q6
		vqmovun.s16	d12, q7
		vst1.8		{q6}, [lr]!
		subs		r6, #16
		bgt		2b
		pld			[r1, r2]
		subs		r5, #1
		add			r0, r0, r2
		add			r1, r1, r2
		bgt		1b
		
		ldmfd	sp!, {r4 - r6, pc}
		
hevc_sao_band_filter_0_w8:
1:	
		mov		r3, r1
		mov		lr, r0
		mov		r6, r4
2:	
		vld1.8		{d4}, [r3]!
		vshr.u8		d5, d4, #3
		vtbl.8		d6, {d0, d1, d2, d3}, d5
		vmovl.u8	q5, d4
		vaddw.s8	q6, q5, d6
		vqmovun.s16	d12, q6
		vst1.8		{d12}, [lr]!
		subs		r6, #8
		bgt		2b
		pld			[r1, r2]
		subs		r5, #1
		add			r0, r0, r2
		add			r1, r1, r2
		bgt		1b
		
		ldmfd	sp!, {r4 - r6, pc}
endfunc
		
.align 4	
Table_idx:
	.byte	 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31
	
	
	