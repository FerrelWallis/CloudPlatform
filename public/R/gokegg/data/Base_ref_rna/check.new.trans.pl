#!usr/bin/perl -w

use strict;

die "usage:perl $0 <gff> <cds.fa> <pep.fa> <output:prefix> <mis number>\n" unless @ARGV == 5;

open C,$ARGV[0]; open A,$ARGV[1];	open B,$ARGV[2];
open O0,">$ARGV[3].gff";	open O1,">$ARGV[3].cds.fa";	open O2,">$ARGV[3].pep.fa";
$/=">";<A>;<B>;
my %seq;	my %last;
while(<A>){
	chomp;my @a=split /\n+/;	
	$seq{$a[0]}=$_;
}
close A;
while(<B>){
	chomp;my @a=split /\n+/;	my @c=split /\s+/,$a[0];
	my $seq=join "",@a[1..$#a];
	$seq=~s/\.$//;
	my $nn=$seq=~tr/\.//;
	if($nn < $ARGV[4]){
		print O2 ">$_";		print O1 ">$seq{$a[0]}";	$last{$c[0]}=1;
	}
}
close B;

$/="\n";	
while(<C>){
	chomp;
	next if(/^#/);
	my @a=split /\t/;	my @b=split /[;=]/,$a[-1];
	if(exists $last{$b[1]}){
		print O0 "$_\n";
	}
}
close C;close O0;close O1;close O2;
