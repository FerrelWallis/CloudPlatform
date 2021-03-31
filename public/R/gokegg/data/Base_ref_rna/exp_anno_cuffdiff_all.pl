#!usr/bin/perl -w

use strict;

die "usage:perl $0 <gene_exp.diff> <anno.info> \n" unless @ARGV == 2 || @ARGV == 1;

open A,$ARGV[0];    my %h;
if(defined $ARGV[1]){
	open B,$ARGV[1];
	while(<B>){
		chomp;my @a=split /\t+/;
		my $inf=join "\t",@a[1..$#a];
		$h{$a[0]}=$inf;
	}
	close B;
}
my $head=<A>;
my %tar;
while(<A>){
	chomp;my @a=split /\t+/;
	my $type="down";
	if($a[8] > $a[7]){
		$type="up";
	}
	my $id=$a[4]."-".$a[5];
	$tar{$id}{$a[0]}=[@a,$type];
	if(exists $h{$a[0]}){
		$tar{$id}{$a[0]}=[@a,$type,$h{$a[0]}];
	}
	else{
		my $mis="-";
		$tar{$id}{$a[0]}=[@a,$type,$mis];
	}
}
close A;

foreach my $k1(sort keys %tar){
	print "gene_id\tlocus\tsample_1\tsample_2\tfpkm_1\tfpkm_2\tlog2(sample_2/sample_1)\tp_value\tq_value\ttype\tfunc_anno\n";
	#print "gene_id\tlocus\tsample_1\tsample_2\tfpkm_value_1\tfpkm_value_2\tlog2(fold_change)\tp_value\tq_value\ttype\tNR_anno\tCOG:anno\tSwissport:anno\tGO:bp\tGO:CC\tGO:MF\tKEGG:anno\n";
	foreach my $k2(sort keys %{$tar{$k1}}){
		print "$tar{$k1}{$k2}[0]\t$tar{$k1}{$k2}[3]\t$tar{$k1}{$k2}[4]\t$tar{$k1}{$k2}[5]\t$tar{$k1}{$k2}[7]\t$tar{$k1}{$k2}[8]\t$tar{$k1}{$k2}[9]\t$tar{$k1}{$k2}[11]\t$tar{$k1}{$k2}[12]\t$tar{$k1}{$k2}[-2]\t$tar{$k1}{$k2}[-1]\n";
	}
}
